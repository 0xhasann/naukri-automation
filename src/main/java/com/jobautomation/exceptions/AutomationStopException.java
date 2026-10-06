package com.jobautomation.exceptions;

/**
 * Signals that the WHOLE run must stop (as opposed to skipping one job).
 * Thrown for: daily limit reached, login failure, security challenge, missing credentials,
 * repeated unknown mandatory questions, unstable browser.
 */
public class AutomationStopException extends AutomationException {
    private final StopReason reason;

    public AutomationStopException(StopReason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public AutomationStopException(StopReason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public StopReason getReason() { return reason; }
}
