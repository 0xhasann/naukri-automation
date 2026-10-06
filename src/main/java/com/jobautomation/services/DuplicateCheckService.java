package com.jobautomation.services;

import com.jobautomation.models.ApplicationRecord;
import com.jobautomation.models.ApplicationStatus;
import com.jobautomation.models.JobDetails;
import com.jobautomation.models.JobListing;
import com.jobautomation.storage.ApplicationHistoryStore;

import java.util.Optional;

/**
 * Never apply twice: by job id / canonical URL, and never to the same title at the same company.
 * Also lets the runner skip jobs that were already evaluated in a previous run.
 */
public class DuplicateCheckService {

    private final ApplicationHistoryStore history;

    public DuplicateCheckService(ApplicationHistoryStore history) {
        this.history = history;
    }

    /** @return a reason when the listing must be skipped, empty when it is new. */
    public Optional<String> reasonToSkip(JobListing listing) {
        String id = listing.getJobId() != null ? listing.getJobId() : canonicalId(listing.getUrl());
        Optional<ApplicationRecord> existing = history.find(id);
        if (existing.isPresent()) {
            ApplicationStatus s = existing.get().getStatus();
            if (s == ApplicationStatus.APPLIED) {
                return Optional.of("Already applied on " + existing.get().getTimestamp());
            }
            if (s == ApplicationStatus.REJECTED || s == ApplicationStatus.SKIPPED) {
                return Optional.of("Already evaluated earlier: " + s + " (" + existing.get().getReason() + ")");
            }
            if (s == ApplicationStatus.REQUIRES_MANUAL_REVIEW) {
                return Optional.of("Flagged for manual review earlier: " + existing.get().getReason());
            }
            // FAILED / DISCOVERED / WOULD_APPLY -> allowed to retry
        }
        return Optional.empty();
    }

    public Optional<String> reasonToSkip(JobDetails details) {
        if (details.isAlreadyApplied()) {
            return Optional.of("Naukri shows this job as already applied");
        }
        if (history.isApplied(details.getJobId())) {
            return Optional.of("Already applied (history)");
        }
        // if (history.isAppliedToSameRole(details.getCompany(), details.getTitle())) {
        //     return Optional.of("Already applied to '" + details.getTitle() + "' at " + details.getCompany());
        // }
        return Optional.empty();
    }

    /** Strips query strings / fragments so the same job reached via different links is one job. */
    public static String canonicalId(String url) {
        if (url == null) {
            return null;
        }
        String u = url;
        int q = u.indexOf('?');
        if (q >= 0) {
            u = u.substring(0, q);
        }
        int h = u.indexOf('#');
        if (h >= 0) {
            u = u.substring(0, h);
        }
        return u.replaceAll("/+$", "");
    }
}
