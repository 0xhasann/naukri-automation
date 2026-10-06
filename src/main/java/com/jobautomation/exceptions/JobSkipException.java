package com.jobautomation.exceptions;

import com.jobautomation.models.ApplicationStatus;

/**
 * Abort the CURRENT job only and continue with the next one. Carries the status that
 * should be recorded in the application history.
 */
public class JobSkipException extends AutomationException {
    private final ApplicationStatus status;

    public JobSkipException(ApplicationStatus status, String message) {
        super(message);
        this.status = status;
    }

    public JobSkipException(ApplicationStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public ApplicationStatus getStatus() { return status; }
}
