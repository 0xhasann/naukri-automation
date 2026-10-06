package com.jobautomation.application;

import com.jobautomation.config.AppConfig;
import com.jobautomation.config.RunMode;
import com.jobautomation.exceptions.JobSkipException;
import com.jobautomation.models.Answer;
import com.jobautomation.models.ApplicationStatus;
import com.jobautomation.models.CandidateProfile;
import com.jobautomation.models.JobDetails;
import com.jobautomation.models.MatchResult;
import com.jobautomation.models.Question;
import com.jobautomation.pages.EasyApplyPage;
import com.jobautomation.pages.JobDetailsPage;
import com.jobautomation.questions.QuestionAnswerService;
import com.jobautomation.services.DailyApplicationLimitService;
import com.jobautomation.utils.ScreenshotUtil;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Performs (LIVE) or rehearses (TEST / DRY_RUN) one application for a job that has ALREADY
 * been judged eligible.
 * <p>
 * Important Naukri behaviour: for jobs without screening questions the Apply button itself
 * submits the application instantly. Therefore the Apply button is clicked ONLY in LIVE mode.
 * TEST mode verifies that Easy Apply is available and rehearses the question engine against
 * the questions Naukri commonly asks, without touching the form.
 */
public class ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);

    /** Typical Naukri chatbot questions, used to rehearse answers in TEST mode. */
    static final List<Question> REHEARSAL_QUESTIONS = List.of(
            Question.text("What is your total years of experience?"),
            Question.text("How many years of experience do you have in Java?"),
            Question.text("How many years of experience do you have in Spring Boot?"),
            Question.choice("Are you willing to relocate to Bengaluru?", List.of("Yes", "No")),
            Question.choice("Are you an immediate joiner?", List.of("Yes", "No")),
            Question.text("What is your notice period (in days)?"),
            Question.text("What is your current CTC (in LPA)?"),
            Question.text("What is your expected CTC (in LPA)?"),
            Question.text("What is your current location?"),
            Question.choice("What is your highest qualification?", List.of("B.Tech", "MCA", "M.Tech", "Other")),
            Question.text("Do you have experience with Kubernetes?")
    );

    public static final class Result {
        public final ApplicationStatus status;
        public final String reason;
        public final String screenshot;
        public final List<String> transcript;

        Result(ApplicationStatus status, String reason, String screenshot, List<String> transcript) {
            this.status = status;
            this.reason = reason;
            this.screenshot = screenshot;
            this.transcript = transcript;
        }
    }

    private final AppConfig config;
    private final WebDriver driver;
    private final CandidateProfile profile;
    private final DailyApplicationLimitService dailyLimit;

    public ApplicationService(AppConfig config, WebDriver driver, CandidateProfile profile, DailyApplicationLimitService dailyLimit) {
        this.config = config;
        this.driver = driver;
        this.profile = profile;
        this.dailyLimit = dailyLimit;
    }

    public Result apply(JobDetailsPage page, JobDetails job, MatchResult match) {
        RunMode mode = config.runMode();
        if (!job.isEasyApplyAvailable()) {
            String why = job.isApplyOnCompanySite()
                    ? "Apply is on the company site (external forms are not automated)"
                    : "No Easy Apply button on this job";
            return new Result(ApplicationStatus.SKIPPED, why, null, List.of());
        }
        if (page.requiresLogin()) {
            throw new JobSkipException(ApplicationStatus.FAILED, "Page asks to login before applying - session may have expired");
        }

        if (mode == RunMode.DRY_RUN) {
            return new Result(ApplicationStatus.WOULD_APPLY, "DRY_RUN: eligible (" + match.getScore() + "%), Apply not clicked", null, List.of());
        }
        if (mode == RunMode.TEST) {
            List<String> rehearsal = rehearse();
            return new Result(ApplicationStatus.WOULD_APPLY,
                    "TEST: eligible (" + match.getScore() + "%), Easy Apply available, Apply NOT clicked (it can submit instantly). Rehearsed answers logged.",
                    null, rehearsal);
        }

        // ---- LIVE
        dailyLimit.assertNotReached();                     // throws DailyLimitReachedException -> run stops
        QuestionAnswerService qa = new QuestionAnswerService(profile);
        log.info("Opening Easy Apply for {} [{}%]", job, match.getScore());
        EasyApplyPage easyApply = page.clickEasyApply();
        EasyApplyPage.Outcome outcome = easyApply.run(mode, qa, job.getJobId());
        switch (outcome) {
            case SUBMITTED: {
                String shot = ScreenshotUtil.capture(driver, "applied_" + job.getJobId());
                if (!page.verifyAppliedAfterReload()) {
                    // Confirmation text was seen but Naukri does not show the job as applied after a reload.
                    // Record it as FAILED (retryable) rather than claiming an application that may not exist.
                    String shot2 = ScreenshotUtil.capture(driver, "apply_unverified_" + job.getJobId());
                    return new Result(ApplicationStatus.FAILED,
                            "Confirmation appeared but the job is not marked as applied after reload - verify manually. Screenshots: " + shot + ", " + shot2,
                            shot2, easyApply.getTranscript());
                }
                log.info("APPLIED (verified after reload): {} (score {}%)", job, match.getScore());
                return new Result(ApplicationStatus.APPLIED,
                        "Submitted via Easy Apply and verified on reload; " + easyApply.getTranscript().size() + " question(s) answered from profile",
                        shot, easyApply.getTranscript());
            }
            case TEST_STOPPED_BEFORE_SUBMIT:
                return new Result(ApplicationStatus.WOULD_APPLY, "Form filled with verified data, not submitted", null, easyApply.getTranscript());
            case NO_CONFIRMATION:
            default: {
                String shot = ScreenshotUtil.capture(driver, "no_confirmation_" + job.getJobId());
                return new Result(ApplicationStatus.FAILED,
                        "Form closed without a confirmation message; verify manually. Screenshot: " + shot,
                        shot, easyApply.getTranscript());
            }
        }
    }

    /** Runs the question engine against typical questions and returns a readable transcript. */
    List<String> rehearse() {
        QuestionAnswerService qa = new QuestionAnswerService(profile);
        List<String> out = new ArrayList<>();
        for (Question q : REHEARSAL_QUESTIONS) {
            Answer a = qa.answer(q);
            out.add("Q: " + q.getText() + " -> " + (a.isAnswered() ? a.getValue() : a.getOutcome() + " (" + a.getExplanation() + ")"));
        }
        return out;
    }
}
