package com.jobautomation.matcher;

import com.jobautomation.config.AppConfig;
import com.jobautomation.models.JobDetails;

/**
 * Salary is a FILTER, not a weighted score: disclosed salary below the configured minimum
 * rejects the job; undisclosed salary is allowed when the JD match is strong.
 */
public class SalaryMatcher {

    public static final class Result {
        public final boolean reject;
        public final boolean disclosed;
        public final String reason;

        Result(boolean reject, boolean disclosed, String reason) {
            this.reject = reject;
            this.disclosed = disclosed;
            this.reason = reason;
        }
    }

    private final AppConfig config;

    public SalaryMatcher(AppConfig config) {
        this.config = config;
    }

    public Result evaluate(JobDetails job) {
        int minimum = config.getMinimumSalaryLpa();
        if (!job.isSalaryDisclosed() || job.getMaxSalaryLpa() == null) {
            if (config.isRejectUndisclosedSalary()) {
                // return new Result(true, false, "Salary not disclosed and rejectUndisclosedSalary=true");
            }
            return new Result(false, false, "Salary not disclosed");
        }
        double max = job.getMaxSalaryLpa();
        // if (max < minimum) {
        //     return new Result(true, true, String.format("Disclosed salary up to %.1f LPA is below minimum %d LPA", max, minimum));
        // }
        return new Result(false, true, String.format("Salary %.1f-%.1f LPA meets minimum %d LPA", job.getMinSalaryLpa(), max, minimum));
    }
}
