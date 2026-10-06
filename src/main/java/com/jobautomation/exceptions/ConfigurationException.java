package com.jobautomation.exceptions;

public class ConfigurationException extends AutomationStopException {
    public ConfigurationException(String message) {
        super(StopReason.CONFIGURATION, message);
    }
    public ConfigurationException(String message, Throwable cause) {
        super(StopReason.CONFIGURATION, message, cause);
    }
}
