package com.jobautomation.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Counters and per-job rows for the end-of-run report. */
public class RunSummary {
    private final LocalDateTime startedAt = LocalDateTime.now();
    private LocalDateTime finishedAt;
    private String mode;
    private int dailyLimit;
    private int appliedBeforeRun;
    private int discovered;
    private int evaluated;
    private int rejected;
    private int skipped;
    private int alreadyProcessed;
    private int applied;
    private int wouldApply;
    private int failed;
    private int manualReview;
    private String stopReason = "Completed";
    private final List<ApplicationRecord> rows = new ArrayList<>();
    private final List<String> citiesSearched = new ArrayList<>();

    public void add(ApplicationRecord r) {
        rows.add(r);
        switch (r.getStatus()) {
            case REJECTED -> rejected++;
            case SKIPPED -> skipped++;
            case APPLIED -> applied++;
            case WOULD_APPLY -> wouldApply++;
            case FAILED -> failed++;
            case REQUIRES_MANUAL_REVIEW -> manualReview++;
            default -> { }
        }
    }

    /** A job already evaluated in an earlier run: counted, but not repeated as a report row. */
    public void addAlreadyProcessed() {
        alreadyProcessed++;
    }

    public int getAlreadyProcessed() { return alreadyProcessed; }

    public int applicationsRemaining() {
        return Math.max(0, dailyLimit - (appliedBeforeRun + applied));
    }

    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public int getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(int dailyLimit) { this.dailyLimit = dailyLimit; }
    public int getAppliedBeforeRun() { return appliedBeforeRun; }
    public void setAppliedBeforeRun(int appliedBeforeRun) { this.appliedBeforeRun = appliedBeforeRun; }
    public int getDiscovered() { return discovered; }
    public void incrementDiscovered(int n) { this.discovered += n; }
    public int getEvaluated() { return evaluated; }
    public void incrementEvaluated() { this.evaluated++; }
    public int getRejected() { return rejected; }
    public int getSkipped() { return skipped; }
    public int getApplied() { return applied; }
    public int getWouldApply() { return wouldApply; }
    public int getFailed() { return failed; }
    public int getManualReview() { return manualReview; }
    public String getStopReason() { return stopReason; }
    public void setStopReason(String stopReason) { this.stopReason = stopReason; }
    public List<ApplicationRecord> getRows() { return rows; }
    public List<String> getCitiesSearched() { return citiesSearched; }
}
