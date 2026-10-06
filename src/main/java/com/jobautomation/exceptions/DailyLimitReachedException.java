package com.jobautomation.exceptions;

public class DailyLimitReachedException extends AutomationStopException {
    public DailyLimitReachedException(String message) {
        super(StopReason.DAILY_LIMIT_REACHED, message);
    }
}
