package com.jobautomation.pages;

import com.jobautomation.components.SecurityChallengeDetector;
import com.jobautomation.exceptions.JobSkipException;
import com.jobautomation.exceptions.SelectorNotFoundException;
import com.jobautomation.models.ApplicationStatus;
import com.jobautomation.models.JobDetails;
import com.jobautomation.models.JobListing;
import com.jobautomation.utils.ScreenshotUtil;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Job details page: extracts the JD and tells the caller which apply path exists. */
public class JobDetailsPage extends BasePage {

    private final SecurityChallengeDetector security;

    public JobDetailsPage(WebDriver driver) {
        super(driver);
        this.security = new SecurityChallengeDetector(driver);
    }

    public JobDetailsPage open(JobListing listing) {
        driver.get(listing.getUrl());
        waitForPageLoad();
        security.assertNoChallenge("job details");
        return this;
    }

    /** Extracts every field; missing optional fields are tolerated, a missing title/description skips the job. */
    public JobDetails extract(JobListing listing) {
        JobDetails d = new JobDetails();
        d.setJobId(listing.getJobId());
        d.setUrl(listing.getUrl());
        try {
            d.setTitle(safeText(waitForVisible("details.title", Duration.ofSeconds(config.getTimeouts().getExplicit()))));
        } catch (SelectorNotFoundException e) {
            String shot = ScreenshotUtil.capture(driver, "jd_title_missing_" + listing.getJobId());
            throw new JobSkipException(ApplicationStatus.FAILED, "Job page did not render a title (" + e.getMessage() + "). Screenshot: " + shot, e);
        }
        d.setCompany(firstNonBlank(getText("details.company"), listing.getCompany()));
        d.setLocation(firstNonBlank(getText("details.location"), listing.getLocation()));
        d.setExperienceText(firstNonBlank(getText("details.experience"), listing.getExperienceText()));
        d.setSalaryText(firstNonBlank(getText("details.salary"), listing.getSalaryText()));
        d.setEmploymentType(getText("details.employmentType"));

        String desc = getText("details.description");
        if (desc.isBlank()) {
            // fall back to the whole page text so the matcher still has something to work on
            desc = pageTextLower();
            log.warn("JD container not found for {}; using full page text", listing.getJobId());
        }
        if (desc.isBlank()) {
            String shot = ScreenshotUtil.capture(driver, "jd_extraction_failed_" + listing.getJobId());
            throw new JobSkipException(ApplicationStatus.FAILED, "Could not extract job description. Screenshot: " + shot);
        }
        d.setDescription(desc);

        List<String> skills = new ArrayList<>();
        for (WebElement e : findAll("details.keySkills")) {
            String s = safeText(e);
            if (!s.isBlank()) {
                skills.add(s);
            }
        }
        d.setKeySkills(skills);

        d.setAlreadyApplied(isDisplayed("details.alreadyApplied"));
        d.setEasyApplyAvailable(!d.isAlreadyApplied() && isDisplayed("details.easyApplyButton"));
        d.setApplyOnCompanySite(!d.isEasyApplyAvailable() && isDisplayed("details.companySiteButton"));
        log.info("JD extracted: {} | exp='{}' | salary='{}' | skills={} | easyApply={} | companySite={} | alreadyApplied={}",
                d, d.getExperienceText(), d.getSalaryText(), skills.size(), d.isEasyApplyAvailable(), d.isApplyOnCompanySite(), d.isAlreadyApplied());
        return d;
    }

    /**
     * Independent confirmation that an application really went through: reload the job page and
     * check that Naukri now shows it as applied (or no longer offers the Apply button).
     */
    public boolean verifyAppliedAfterReload() {
        try {
            driver.navigate().refresh();
            waitForPageLoad();
            security.assertNoChallenge("post-apply verification");
            if (waitForVisibleQuietly("details.alreadyApplied", Duration.ofSeconds(config.getTimeouts().getShort()))) {
                return true;
            }
            if (waitForVisibleQuietly("confirmation.success", Duration.ofSeconds(2))) {
                return true;
            }
            boolean applyStillOffered = isDisplayed("details.easyApplyButton");
            log.warn("Post-apply verification: 'already applied' marker not found; Apply button still offered = {}", applyStillOffered);
            return !applyStillOffered;
        } catch (com.jobautomation.exceptions.AutomationStopException stop) {
            throw stop;
        } catch (RuntimeException e) {
            log.warn("Post-apply verification could not run: {}", e.getMessage());
            return false;
        }
    }

    public boolean requiresLogin() {
        return isDisplayed("details.loginToApply");
    }

    /** Clicks the Easy Apply button. Caller has already decided the job is eligible. */
    public EasyApplyPage clickEasyApply() {
        click("details.easyApplyButton");
        return new EasyApplyPage(driver);
    }

    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : (b == null ? "" : b);
    }
}
