package com.jobautomation.exceptions;

public class MissingCredentialsException extends AutomationStopException {
    public MissingCredentialsException(String message) {
        super(StopReason.MISSING_CREDENTIALS, message);
    }
}
