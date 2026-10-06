package com.jobautomation.matcher;

import com.jobautomation.models.CandidateProfile;
import com.jobautomation.models.JobDetails;

/**
 * Compares the JD experience range with the candidate's VERIFIED total experience.
 * The search filter may target 4+ years; this class never pretends the candidate has more
 * than the profile states.
 */
public class ExperienceMatcher {

    public static final class Result {
        public final double score;
        public final boolean hardReject;
        public final String reason;

        Result(double score, boolean hardReject, String reason) {
            this.score = score;
            this.hardReject = hardReject;
            this.reason = reason;
        }
    }

    /** JD minimum may exceed the verified experience by at most this much before rejecting. */
    private final double tolerance;

    public ExperienceMatcher() {
        this(1.0);
    }

    public ExperienceMatcher(double toleranceYears) {
        this.tolerance = toleranceYears;
    }

    public Result evaluate(JobDetails job, CandidateProfile profile) {
        Double mine = profile.getTotalExperienceYears();
        Integer min = job.getMinExperienceYears();
        Integer max = job.getMaxExperienceYears();

        if (mine == null) {
            return new Result(0.5, false, "Candidate experience not configured");
        }
        if (min == null) {
            return new Result(0.7, false, "JD does not state an experience range");
        }
        if (min > mine + tolerance) {
            return new Result(0.0, true, String.format("JD requires %d+ years, verified experience is %.1f", min, mine));
        }
        if (max != null && max < mine - 2) {
            return new Result(0.4, false, String.format("JD targets %d-%d years, candidate is more senior (%.1f)", min, max, mine));
        }
        if (min <= mine) {
            if (max == null || mine <= max) {
                return new Result(1.0, false, "Experience within JD range");
            }
            return new Result(0.7, false, "Slightly above JD maximum");
        }
        // min is within tolerance above verified experience (e.g. JD 4+, candidate 3.7)
        return new Result(0.8, false, String.format("JD minimum %d is within %.1f year of verified %.1f", min, tolerance, mine));
    }
}
