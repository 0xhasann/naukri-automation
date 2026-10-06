package com.jobautomation.components;

import com.jobautomation.exceptions.SecurityChallengeException;
import com.jobautomation.pages.BasePage;
import com.jobautomation.utils.ScreenshotUtil;
import org.openqa.selenium.WebDriver;

import java.util.Locale;

/**
 * Detects CAPTCHA / OTP / MFA / anti-bot / access-denied pages.
 * <p>
 * Policy: detection only. There is deliberately no code path that attempts to solve, wait out,
 * or circumvent a challenge. On detection the run stops with a screenshot and the browser is
 * left open (in non-headless mode) so a human can take over.
 */
public class SecurityChallengeDetector extends BasePage {

    /** Path fragments that only interstitial / verification pages use ("otp" alone would match "hotpoint"). */
    private static final java.util.regex.Pattern SUSPICIOUS_URL = java.util.regex.Pattern.compile(
            "captcha|/challenge|otp-verif|verify-otp|/otp[/?]|/otp$|/verify[/?]|/verify$|/blocked|access-denied");

    private static final String[] TEXT_MARKERS = {
            "captcha", "verify you are human", "verify you're human", "are you a robot", "i'm not a robot",
            "enter otp", "one time password", "one-time password", "verification code", "two-factor", "2-step verification",
            "access denied", "unusual traffic", "unusual activity", "suspicious activity", "blocked", "rate limit",
            "security check", "checking your browser", "attention required"
    };

    public SecurityChallengeDetector(WebDriver driver) {
        super(driver);
    }

    /** @return a description of the challenge, or null when the page looks normal. */
    public String detect() {
        try {
            String url = safeUrl().toLowerCase(Locale.ROOT);
            if (SUSPICIOUS_URL.matcher(url).find()) {
                return "Suspicious URL: " + url;
            }
            if (isDisplayed("security.captcha")) {
                return "CAPTCHA element present";
            }
            if (isDisplayed("security.otp")) {
                return "OTP / verification-code input present";
            }
            if (isDisplayed("security.accessDenied")) {
                return "Access denied / bot-check page";
            }
            String body = pageTextLower();
            if (!body.isEmpty() && body.length() < 4000) {
                // Short pages are typical of interstitials; long pages (job lists) mention these words legitimately
                for (String m : TEXT_MARKERS) {
                    if (body.contains(m)) {
                        return "Page text mentions '" + m + "'";
                    }
                }
            }
            return null;
        } catch (RuntimeException e) {
            log.debug("Security detection skipped: {}", e.getMessage());
            return null;
        }
    }

    /** Throws {@link SecurityChallengeException} when a challenge is present. */
    public void assertNoChallenge(String context) {
        String reason = detect();
        if (reason != null) {
            String shot = ScreenshotUtil.capture(driver, "security_challenge_" + context);
            log.error("SECURITY CHALLENGE during {}: {}. Screenshot: {}. Stopping - manual intervention required. No bypass will be attempted.",
                    context, reason, shot);
            throw new SecurityChallengeException("Security challenge during " + context + ": " + reason
                    + ". Automation stopped; complete the verification manually and restart.");
        }
    }
}
