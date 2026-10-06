package com.jobautomation.models;

public enum ApplicationStatus {
    DISCOVERED,
    REJECTED,
    SKIPPED,
    APPLIED,
    FAILED,
    REQUIRES_MANUAL_REVIEW,
    /** TEST / DRY_RUN result: would have applied, but submission is disabled in this mode. */
    WOULD_APPLY;

    public boolean isTerminalApplied() {
        return this == APPLIED;
    }
}
