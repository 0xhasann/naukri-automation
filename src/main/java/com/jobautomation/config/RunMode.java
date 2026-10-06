package com.jobautomation.config;

/**
 * Execution mode of the automation.
 *
 * <ul>
 *   <li>{@link #DRY_RUN} - search, extract JD, score, print decision. Never opens Easy Apply.</li>
 *   <li>{@link #TEST} - additionally walks the Easy Apply flow and fills verified answers. Never submits.</li>
 *   <li>{@link #LIVE} - full flow, submits eligible applications, enforces the daily limit.</li>
 * </ul>
 */
public enum RunMode {
    DRY_RUN,
    TEST,
    LIVE;

    public boolean opensEasyApply() {
        return this != DRY_RUN;
    }

    public boolean submits() {
        return this == LIVE;
    }

    public static RunMode parse(String value, RunMode fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String v = value.trim().toUpperCase().replace('-', '_');
        if ("TEST_MODE".equals(v)) {
            return TEST;
        }
        if ("LIVE_MODE".equals(v)) {
            return LIVE;
        }
        if ("DRY".equals(v) || "DRYRUN".equals(v)) {
            return DRY_RUN;
        }
        return RunMode.valueOf(v);
    }
}
