package com.jobautomation.exceptions;

/** None of the fallback locators for a key resolved. */
public class SelectorNotFoundException extends AutomationException {
    private final String selectorKey;

    public SelectorNotFoundException(String selectorKey, String message) {
        super(message);
        this.selectorKey = selectorKey;
    }

    public String getSelectorKey() { return selectorKey; }
}
