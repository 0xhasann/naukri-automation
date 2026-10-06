package com.jobautomation.matcher;

import com.jobautomation.config.AppConfig;
import com.jobautomation.models.CandidateProfile;
import com.jobautomation.models.JobDetails;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Scores the job location against the configured priority list and relocation preference. */
public class LocationMatcher {

    private static final Map<String, List<String>> ALIASES = Map.of(
            "bengaluru", List.of("bengaluru", "bangalore", "bengalooru"),
            "gurugram", List.of("gurugram", "gurgaon"),
            "delhi ncr", List.of("delhi", "ncr", "new delhi", "noida", "gurgaon", "gurugram", "faridabad", "ghaziabad"),
            "hyderabad", List.of("hyderabad", "secunderabad"),
            "pune", List.of("pune"),
            "chennai", List.of("chennai"),
            "mumbai", List.of("mumbai", "navi mumbai", "thane"),
            "noida", List.of("noida"),
            "remote", List.of("remote", "work from home", "wfh", "anywhere in india")
    );

    public static final class Result {
        public final double score;
        public final String reason;

        Result(double score, String reason) {
            this.score = score;
            this.reason = reason;
        }
    }

    private final AppConfig config;

    public LocationMatcher(AppConfig config) {
        this.config = config;
    }

    public Result evaluate(JobDetails job, CandidateProfile profile) {
        String loc = job.getLocation() == null ? "" : job.getLocation().toLowerCase(Locale.ROOT);
        String mode = job.getWorkMode() == null ? "" : job.getWorkMode().toLowerCase(Locale.ROOT);
        if (loc.isBlank() && mode.isBlank()) {
            return new Result(0.5, "Location not stated");
        }
        if (mode.contains("remote") || matches(loc, "remote")) {
            return new Result(1.0, "Remote role");
        }
        List<String> priority = config.getLocationPriority();
        for (int i = 0; i < priority.size(); i++) {
            if (matches(loc, priority.get(i))) {
                double score = i == 0 ? 1.0 : Math.max(0.7, 1.0 - i * 0.05);
                return new Result(score, "Priority city #" + (i + 1) + " (" + priority.get(i) + ")");
            }
        }
        if (profile.isLocationPreferred(job.getLocation())) {
            return new Result(0.8, "Candidate-preferred location");
        }
        if (mode.contains("hybrid")) {
            return new Result(0.6, "Hybrid role outside priority cities");
        }
        if (Boolean.TRUE.equals(profile.getRelocationWilling())) {
            return new Result(0.4, "Outside priority cities; relocation acceptable");
        }
        return new Result(0.0, "Outside priority cities; relocation not acceptable");
    }

    public static boolean matches(String jobLocationLower, String city) {
        String key = city.toLowerCase(Locale.ROOT);
        List<String> aliases = ALIASES.getOrDefault(key, List.of(key));
        for (String a : aliases) {
            if (jobLocationLower.contains(a)) {
                return true;
            }
        }
        return false;
    }
}
