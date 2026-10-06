package com.jobautomation.exceptions;

/**
 * CAPTCHA / OTP / MFA / anti-bot page detected. The framework NEVER attempts to solve or
 * bypass these; it screenshots, logs, preserves state and stops for manual intervention.
 */
public class SecurityChallengeException extends AutomationStopException {
    public SecurityChallengeException(String message) {
        super(StopReason.SECURITY_CHALLENGE, message);
    }
}
