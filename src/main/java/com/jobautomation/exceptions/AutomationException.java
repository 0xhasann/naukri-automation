package com.jobautomation.exceptions;

/** Base class for every framework exception. */
public class AutomationException extends RuntimeException {
    public AutomationException(String message) { super(message); }
    public AutomationException(String message, Throwable cause) { super(message, cause); }
}
