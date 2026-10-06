package com.jobautomation.services;

import com.jobautomation.application.ApplicationService;
import com.jobautomation.config.AppConfig;
import com.jobautomation.config.ConfigLoader;
import com.jobautomation.config.Credentials;
import com.jobautomation.config.ProfileLoader;
import com.jobautomation.config.RunMode;
import com.jobautomation.driver.DriverManager;
import com.jobautomation.exceptions.AutomationStopException;
import com.jobautomation.exceptions.JobSkipException;
import com.jobautomation.exceptions.SecurityChallengeException;
import com.jobautomation.components.SecurityChallengeDetector;
import com.jobautomation.exceptions.StopReason;
import com.jobautomation.exceptions.UnknownQuestionException;
import com.jobautomation.matcher.JobMatcher;
import com.jobautomation.models.ApplicationRecord;
import com.jobautomation.models.ApplicationStatus;
import com.jobautomation.models.CandidateProfile;
import com.jobautomation.models.Decision;
import com.jobautomation.models.JobDetails;
import com.jobautomation.models.JobListing;
import com.jobautomation.models.MatchResult;
import com.jobautomation.models.RunSummary;
import com.jobautomation.pages.HomePage;
import com.jobautomation.pages.JobDetailsPage;
import com.jobautomation.pages.LoginPage;
import com.jobautomation.storage.ApplicationHistoryStore;
import com.jobautomation.utils.RetryUtil;
import com.jobautomation.utils.ScreenshotUtil;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * End-to-end orchestration:
 * <pre>
 * load config/profile/history -> check daily limit -> browser -> login
 *   -> for each city (priority order) -> for each keyword -> for each listing:
 *        duplicate check -> open -> extract JD -> match -> (apply | skip | reject) -> record
 * -> report
 * </pre>
 * Failure policy ("it must not fail"):
 * <ul>
 *   <li>Any problem with ONE job (stale DOM, timeout, unexpected layout, unanswerable question)
 *       is caught, screenshotted, recorded as FAILED / SKIPPED / REQUIRES_MANUAL_REVIEW, and the
 *       run moves to the next job.</li>
 *   <li>A dead browser session is restarted and the session re-logged in once; history makes
 *       the restart resume where it left off.</li>
 *   <li>Only the hard stop conditions end the run: daily limit, login failure, CAPTCHA/OTP,
 *       missing credentials/data, repeated unknown mandatory questions, an unrecoverable browser.</li>
 *   <li>State (history) is persisted after every job, so a crash or Ctrl-C loses nothing and
 *       the next run never re-applies.</li>
 * </ul>
 */
public class JobAutomationRunner {

    private static final Logger log = LoggerFactory.getLogger(JobAutomationRunner.class);

    private final AppConfig config;
    private final CandidateProfile profile;
    private final ApplicationHistoryStore history;
    private final DailyApplicationLimitService dailyLimit;
    private final DuplicateCheckService duplicates;
    private final JobMatcher matcher;
    private final ReportService reports;
    private final RunSummary summary = new RunSummary();

    private WebDriver driver;
    private JobSearchService searchService;
    private ApplicationService applicationService;
    private int unknownQuestionCount;
    private int consecutiveFailures;
    private boolean browserRestartedOnce;
    private volatile boolean stopRequested;

    public JobAutomationRunner() {
        this(ConfigLoader.get());
    }

    public JobAutomationRunner(AppConfig config) {
        this.config = config;
        this.profile = ProfileLoader.load(config);
        this.history = new ApplicationHistoryStore(Paths.get(config.getPaths().getApplicationHistory()));
        this.dailyLimit = new DailyApplicationLimitService(history, config.getDailyApplicationLimit());
        this.duplicates = new DuplicateCheckService(history);
        this.matcher = new JobMatcher(config);
        this.reports = new ReportService(config);
    }

    public RunSummary getSummary() {
        return summary;
    }

    public void requestStop() {
        stopRequested = true;
    }

    // ------------------------------------------------------------------ main flow

    public RunSummary run() {
        RunMode mode = config.runMode();
        summary.setMode(mode.name());
        summary.setDailyLimit(config.getDailyApplicationLimit());
        summary.setAppliedBeforeRun(dailyLimit.applicationsToday());
        log.info("==== Naukri Java Developer automation starting | mode={} | daily limit {} ({} applied today) | cities={} | keywords={}",
                mode, config.getDailyApplicationLimit(), summary.getAppliedBeforeRun(), config.getLocationPriority(), config.getSearchKeywords().size());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stopRequested = true;
            history.save();
        }, "history-flush"));

        try {
            if (mode.submits() && dailyLimit.isReached()) {
                throw new AutomationStopException(StopReason.DAILY_LIMIT_REACHED,
                        "Daily application limit reached (" + dailyLimit.applicationsToday() + "/" + dailyLimit.getLimit() + "). Nothing to do.");
            }
            Credentials credentials = Credentials.fromEnvironment();     // fail fast, before the browser
            startBrowserAndLogin(credentials);

            cityLoop:
            for (String city : config.getLocationPriority()) {
                if (stopRequested) {
                    break;
                }
                summary.getCitiesSearched().add(city);
                int eligibleInCity = 0;
                int keywordsTried = 0;
                log.info("---- City: {} ----", city);
                for (String keyword : config.getSearchKeywords()) {
                    if (stopRequested) {
                        break cityLoop;
                    }
                    if (mode.submits() && dailyLimit.isReached()) {
                        throw new AutomationStopException(StopReason.DAILY_LIMIT_REACHED, "Daily application limit reached.");
                    }
                    keywordsTried++;
                    List<JobListing> listings = searchSafely(city, keyword, credentials);
                    summary.incrementDiscovered(listings.size());
                    history.markSearchProgress(city, keyword, 1);
                    for (JobListing listing : listings) {
                        if (stopRequested) {
                            break cityLoop;
                        }
                        ApplicationStatus status = processJobSafely(listing, credentials);
                        if (status == ApplicationStatus.APPLIED || status == ApplicationStatus.WOULD_APPLY) {
                            eligibleInCity++;
                        }
                        if (mode.submits() && dailyLimit.isReached()) {
                            throw new AutomationStopException(StopReason.DAILY_LIMIT_REACHED, "Daily application limit reached.");
                        }
                    }
                    // Insufficient opportunities in this city after a fair number of searches -> next city
                    if (keywordsTried >= 4 && eligibleInCity < config.getMinStrongMatchesPerCity()) {
                        log.info("Only {} strong matches in {} after {} keyword searches (< {}); moving to the next city",
                                eligibleInCity, city, keywordsTried, config.getMinStrongMatchesPerCity());
                        break;
                    }
                }
                log.info("---- {} done: {} strong matches ----", city, eligibleInCity);
            }
            summary.setStopReason(stopRequested ? "Stopped by request" : "All cities and keywords processed");
        } catch (AutomationStopException e) {
            summary.setStopReason(e.getReason() + ": " + e.getMessage());
            if (e.getReason() == StopReason.DAILY_LIMIT_REACHED) {
                log.info("Daily application limit reached. {}", e.getMessage());
            } else {
                log.error("RUN STOPPED ({}): {}", e.getReason(), e.getMessage());
            }
        } catch (RuntimeException e) {
            summary.setStopReason("Unexpected error: " + e);
            log.error("RUN STOPPED by unexpected error", e);
            ScreenshotUtil.capture(driver, "unexpected_error");
        } finally {
            history.save();
            reports.generate(summary);
            logTotals();
            boolean keepOpen = !config.isHeadless() && summary.getStopReason() != null
                    && summary.getStopReason().startsWith(StopReason.SECURITY_CHALLENGE.name());
            if (keepOpen) {
                log.warn("Browser left open for manual verification (security challenge). Close it yourself when done.");
            } else {
                DriverManager.quit();
            }
        }
        return summary;
    }

    // ------------------------------------------------------------------ per-job processing with isolation

    /** Never throws for a single-job problem; only stop-the-run exceptions propagate. */
    private ApplicationStatus processJobSafely(JobListing listing, Credentials credentials) {
        try {
            ApplicationStatus s = processJob(listing);
            consecutiveFailures = 0;
            return s;
        } catch (SecurityChallengeException challenge) {
            if (waitForManualIntervention(challenge)) {
                return processJobSafely(listing, credentials);   // human cleared it - retry this job once
            }
            throw challenge;
        } catch (AutomationStopException stop) {
            throw stop;
        } catch (JobSkipException skip) {
            log.warn("{} -> {}: {}", listing, skip.getStatus(), skip.getMessage());
            record(ApplicationRecord.of(listing, skip.getStatus(), skip.getMessage()));
            if (skip instanceof UnknownQuestionException) {
                unknownQuestionCount++;
                if (unknownQuestionCount >= config.getMaxUnknownQuestionsBeforeStop()) {
                    throw new AutomationStopException(StopReason.REPEATED_UNKNOWN_QUESTIONS,
                            unknownQuestionCount + " applications hit unknown mandatory questions. Review screenshots/ and extend the question engine or profile.");
                }
            }
            return skip.getStatus();
        } catch (WebDriverException | IllegalStateException e) {
            consecutiveFailures++;
            String shot = ScreenshotUtil.capture(driver, "job_failure_" + listing.getJobId());
            log.error("{} -> FAILED ({}): {}", listing, e.getClass().getSimpleName(), firstLine(e.getMessage()));
            record(ApplicationRecord.of(listing, ApplicationStatus.FAILED, e.getClass().getSimpleName() + ": " + firstLine(e.getMessage()) + (shot != null ? " (screenshot " + shot + ")" : "")));
            recoverBrowserIfDead(credentials);
            failIfTooManyConsecutiveFailures();
            return ApplicationStatus.FAILED;
        } catch (RuntimeException e) {
            consecutiveFailures++;
            log.error("{} -> FAILED (unexpected {}): {}", listing, e.getClass().getSimpleName(), e.getMessage(), e);
            record(ApplicationRecord.of(listing, ApplicationStatus.FAILED, "Unexpected " + e.getClass().getSimpleName() + ": " + firstLine(e.getMessage())));
            failIfTooManyConsecutiveFailures();
            return ApplicationStatus.FAILED;
        }
    }

    private ApplicationStatus processJob(JobListing listing) {
        Optional<String> dup = duplicates.reasonToSkip(listing);
        if (dup.isPresent()) {
            log.info("{} -> already processed: {}", listing, dup.get());
            summary.addAlreadyProcessed();                 // history already has the record; keep the report readable
            return ApplicationStatus.SKIPPED;
        }
        summary.incrementEvaluated();

        JobDetailsPage page = new JobDetailsPage(driver).open(listing);
        JobDetails details = page.extract(listing);
        details.setJobId(listing.getJobId());

        Optional<String> dup2 = duplicates.reasonToSkip(details);
        if (dup2.isPresent()) {
            log.info("{} -> SKIP: {}", details, dup2.get());
            record(ApplicationRecord.of(details, ApplicationStatus.SKIPPED, null, dup2.get()));
            return ApplicationStatus.SKIPPED;
        }

        MatchResult match = matcher.match(details, profile);
        log.info("Match for {}:\n{}", details, match.summary());
        printDryRunBlock(details, match);

        if (match.getDecision() == Decision.REJECT) {
            record(ApplicationRecord.of(details, ApplicationStatus.REJECTED, match.getScore(), String.join(" | ", match.getReasons())));
            return ApplicationStatus.REJECTED;
        }
        if (match.getDecision() == Decision.REVIEW) {
            record(ApplicationRecord.of(details, ApplicationStatus.REQUIRES_MANUAL_REVIEW, match.getScore(),
                    "Score " + match.getScore() + "% in review band. Missing: " + match.getMissingSkills()));
            return ApplicationStatus.REQUIRES_MANUAL_REVIEW;
        }

        ApplicationService.Result result = applicationService.apply(page, details, match);
        ApplicationRecord rec = ApplicationRecord.of(details, result.status, match.getScore(), result.reason);
        rec.setSearchCity(listing.getSearchCity());
        rec.setScreenshot(result.screenshot);
        record(rec);
        if (!result.transcript.isEmpty()) {
            result.transcript.forEach(line -> log.info("   {}", line));
        }
        return result.status;
    }

    // ------------------------------------------------------------------ search with isolation

    private List<JobListing> searchSafely(String city, String keyword, Credentials credentials) {
        try {
            List<JobListing> l = searchService.search(city, keyword);
            consecutiveFailures = 0;
            return l;
        } catch (SecurityChallengeException challenge) {
            if (waitForManualIntervention(challenge)) {
                return searchSafely(city, keyword, credentials);
            }
            throw challenge;
        } catch (AutomationStopException stop) {
            throw stop;
        } catch (RuntimeException e) {
            consecutiveFailures++;
            log.error("Search '{}' in {} failed: {}", keyword, city, firstLine(e.getMessage()));
            ScreenshotUtil.capture(driver, "search_failure_" + city + "_" + keyword);
            recoverBrowserIfDead(credentials);
            failIfTooManyConsecutiveFailures();
            return List.of();
        }
    }

    // ------------------------------------------------------------------ browser lifecycle & recovery

    private void startBrowserAndLogin(Credentials credentials) {
        driver = DriverManager.start(config);
        searchService = new JobSearchService(config, driver);
        applicationService = new ApplicationService(config, driver, profile, dailyLimit);
        HomePage home = new LoginPage(driver).open().login(credentials);
        if (!home.isLoggedIn()) {
            throw new AutomationStopException(StopReason.LOGIN_FAILED, "Login not confirmed.");
        }
    }

    private void recoverBrowserIfDead(Credentials credentials) {
        if (DriverManager.isAlive()) {
            return;
        }
        if (browserRestartedOnce) {
            throw new AutomationStopException(StopReason.BROWSER_UNSTABLE, "Browser died twice; stopping. History is saved - restart to resume.");
        }
        browserRestartedOnce = true;
        log.warn("Browser session is dead - restarting once and logging in again (history preserved)");
        RetryUtil.sleepQuietly(2000);
        startBrowserAndLogin(credentials);
    }

    private boolean manualInterventionUsed;

    /**
     * CAPTCHA / OTP handling: the framework never solves or bypasses anything. In non-headless mode
     * it pauses (once per run) so that YOU can complete the verification in the open browser window;
     * when the challenge is gone and the session is still logged in, the run resumes. Otherwise it stops.
     */
    private boolean waitForManualIntervention(SecurityChallengeException challenge) {
        int minutes = config.getManualInterventionWaitMinutes();
        if (config.isHeadless() || minutes <= 0 || manualInterventionUsed || !DriverManager.isAlive()) {
            return false;
        }
        manualInterventionUsed = true;
        log.warn("");
        log.warn("************************************************************************");
        log.warn("*  SECURITY CHALLENGE: {}", challenge.getMessage());
        log.warn("*  Please complete the CAPTCHA / OTP in the open browser window.");
        log.warn("*  Waiting up to {} minute(s); the run resumes automatically once it is cleared.", minutes);
        log.warn("************************************************************************");
        SecurityChallengeDetector detector = new SecurityChallengeDetector(driver);
        HomePage home = new HomePage(driver);
        long deadline = System.currentTimeMillis() + minutes * 60_000L;
        while (System.currentTimeMillis() < deadline) {
            RetryUtil.sleepQuietly(5_000);
            if (!DriverManager.isAlive()) {
                return false;
            }
            if (detector.detect() == null && home.isLoggedIn()) {
                log.warn("Challenge cleared by the user - resuming.");
                return true;
            }
        }
        log.error("Challenge not cleared within {} minute(s) - stopping.", minutes);
        return false;
    }

    private void failIfTooManyConsecutiveFailures() {
        if (consecutiveFailures >= config.getMaxConsecutiveFailuresBeforeStop()) {
            throw new AutomationStopException(StopReason.BROWSER_UNSTABLE,
                    consecutiveFailures + " consecutive failures - Naukri layout may have changed or the session is broken. Check screenshots/ and selectors.yaml.");
        }
    }

    // ------------------------------------------------------------------ helpers

    private void record(ApplicationRecord r) {
        history.upsert(r);
        summary.add(r);
    }

    private void printDryRunBlock(JobDetails d, MatchResult m) {
        if (config.runMode() != RunMode.DRY_RUN) {
            return;
        }
        String block = "\n" +
                "JOB:        " + d.getTitle() + "\n" +
                "COMPANY:    " + d.getCompany() + "\n" +
                "LOCATION:   " + d.getLocation() + "\n" +
                "SALARY:     " + (d.isSalaryDisclosed() ? d.getSalaryText() : "Not disclosed") + "\n" +
                "EXPERIENCE: " + d.getExperienceText() + "\n" +
                "MATCH:      " + m.getScore() + "%\n" +
                "MATCHED:    " + String.join(", ", m.getMatchedSkills()) + "\n" +
                "MISSING:    " + String.join(", ", m.getMissingSkills()) + "\n" +
                "DECISION:   " + m.getDecision() + "\n";
        log.info(block);
    }

    private void logTotals() {
        log.info("==== RUN SUMMARY ({}) ====", summary.getMode());
        log.info("Discovered {} | Already processed earlier {} | Evaluated {} | Rejected {} | Skipped {} | Applied {} | Would apply {} | Failed {} | Manual review {} | Remaining today {}/{}",
                summary.getDiscovered(), summary.getAlreadyProcessed(), summary.getEvaluated(), summary.getRejected(), summary.getSkipped(), summary.getApplied(),
                summary.getWouldApply(), summary.getFailed(), summary.getManualReview(), summary.applicationsRemaining(), summary.getDailyLimit());
        log.info("Stop reason: {}", summary.getStopReason());
    }

    private static String firstLine(String s) {
        if (s == null) {
            return "";
        }
        int i = s.indexOf('\n');
        return i < 0 ? s : s.substring(0, i);
    }
}
