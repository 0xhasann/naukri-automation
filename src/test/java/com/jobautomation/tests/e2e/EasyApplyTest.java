package com.jobautomation.tests.e2e;

import com.jobautomation.config.Credentials;
import com.jobautomation.config.RunMode;
import com.jobautomation.exceptions.MissingCredentialsException;
import com.jobautomation.models.RunSummary;
import com.jobautomation.services.JobAutomationRunner;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Full pipeline through {@link JobAutomationRunner}. The mode comes from {@code -Dmode}:
 * DRY_RUN (default) and TEST never click Apply; LIVE applies and honours the daily limit.
 * This test manages its own browser (the runner does), so it does not extend BaseE2ETest.
 */
public class EasyApplyTest {

    @Test(groups = "e2e", description = "End-to-end run in the configured mode")
    public void runPipeline() {
        try {
            Credentials.fromEnvironment();
        } catch (MissingCredentialsException e) {
            throw new SkipException("Skipping browser test: " + e.getMessage());
        }
        JobAutomationRunner runner = new JobAutomationRunner();
        RunSummary summary = runner.run();
        System.out.println("Stop reason: " + summary.getStopReason());
        Assert.assertNotNull(summary.getStopReason());
        Assert.assertFalse(summary.getStopReason().startsWith("LOGIN_FAILED"), summary.getStopReason());
        Assert.assertFalse(summary.getStopReason().startsWith("SECURITY_CHALLENGE"), summary.getStopReason());
        if (RunMode.parse(System.getProperty("mode"), RunMode.DRY_RUN) != RunMode.LIVE) {
            Assert.assertEquals(summary.getApplied(), 0, "non-LIVE modes must never submit");
        } else {
            Assert.assertTrue(summary.getApplied() <= summary.getDailyLimit(), "daily limit exceeded");
        }
    }
}
