package com.jobautomation.exceptions;

public class LoginFailedException extends AutomationStopException {
    public LoginFailedException(String message) {
        super(StopReason.LOGIN_FAILED, message);
    }
    public LoginFailedException(String message, Throwable cause) {
        super(StopReason.LOGIN_FAILED, message, cause);
    }
}
