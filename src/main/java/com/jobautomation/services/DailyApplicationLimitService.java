package com.jobautomation.services;

import com.jobautomation.exceptions.DailyLimitReachedException;
import com.jobautomation.storage.ApplicationHistoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

/**
 * Enforces the configured daily application limit. The count comes from the persistent
 * history, so it survives restarts and crashes; nothing is kept only in memory.
 */
public class DailyApplicationLimitService {

    private static final Logger log = LoggerFactory.getLogger(DailyApplicationLimitService.class);

    private final ApplicationHistoryStore history;
    private final int limit;

    public DailyApplicationLimitService(ApplicationHistoryStore history, int dailyLimit) {
        this.history = history;
        this.limit = dailyLimit;
    }

    public int applicationsToday() {
        return history.appliedOn(LocalDate.now());
    }

    public int remaining() {
        return Math.max(0, limit - applicationsToday());
    }

    public boolean isReached() {
        return applicationsToday() >= limit;
    }

    /** Call BEFORE every application attempt. */
    public void assertNotReached() {
        int today = applicationsToday();
        if (today >= limit) {
            log.info("Daily application limit reached ({}/{}).", today, limit);
            throw new DailyLimitReachedException("Daily application limit reached (" + today + "/" + limit + ").");
        }
        log.debug("Applications today: {}/{}", today, limit);
    }

    public int getLimit() {
        return limit;
    }
}
