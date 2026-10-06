package com.jobautomation.exceptions;

import com.jobautomation.models.ApplicationStatus;

/** A mandatory form field needs candidate data that is not configured. Application is aborted, never guessed. */
public class MissingProfileDataException extends JobSkipException {
    public MissingProfileDataException(String message) {
        super(ApplicationStatus.REQUIRES_MANUAL_REVIEW, message);
    }
}
