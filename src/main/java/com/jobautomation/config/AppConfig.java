package com.jobautomation.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Strongly typed view of {@code config.yaml}. Populated by {@link ConfigLoader}.
 * All fields have safe defaults so a partially filled YAML still produces a valid config.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfig {

    private String browser = "chrome";
    private boolean headless = false;
    private String mode = "LIVE";
    private String baseUrl = "https://www.naukri.com";

    private int dailyApplicationLimit = 50;
    private int maxJobsPerSearch = 40;
    private int maxSearchPagesPerKeyword = 2;
    private int minStrongMatchesPerCity = 8;
    private int maxUnknownQuestionsBeforeStop = 5;
    private int maxConsecutiveFailuresBeforeStop = 8;
    private int manualInterventionWaitMinutes = 10;

    private Matching matching = new Matching();
    private int minimumSalaryLpa = 15;
    private boolean rejectUndisclosedSalary = false;
    private ExperienceRange targetExperience = new ExperienceRange();

    private List<String> locationPriority = new ArrayList<>(List.of("Bengaluru", "Hyderabad", "Pune", "Chennai", "Gurugram", "Delhi NCR"));
    private Map<String, String> locationSlugs = new LinkedHashMap<>();
    private List<String> searchKeywords = new ArrayList<>(List.of("Java Developer"));
    private List<String> preferredRoles = new ArrayList<>();
    private List<String> rejectedRoleKeywords = new ArrayList<>();
    private Companies companies = new Companies();
    private Timeouts timeouts = new Timeouts();
    private Retry retry = new Retry();
    private Paths paths = new Paths();

    // ------------------------------------------------------------------ nested types

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Matching {
        private int eligibleScore = 60;
        private int reviewScore = 60;
        private boolean allowReviewBandApplications = false;
        private Weights weights = new Weights();

        public int getEligibleScore() { return eligibleScore; }
        public void setEligibleScore(int eligibleScore) { this.eligibleScore = eligibleScore; }
        public int getReviewScore() { return reviewScore; }
        public void setReviewScore(int reviewScore) { this.reviewScore = reviewScore; }
        public boolean isAllowReviewBandApplications() { return allowReviewBandApplications; }
        public void setAllowReviewBandApplications(boolean v) { this.allowReviewBandApplications = v; }
        public Weights getWeights() { return weights; }
        public void setWeights(Weights weights) { this.weights = weights; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Weights {
        private int role = 25;
        private int javaBackend = 25;
        private int springMicroservices = 15;
        private int databaseMessaging = 10;
        private int cloudDevops = 10;
        private int experience = 10;
        private int location = 5;

        public int total() {
            return role + javaBackend + springMicroservices + databaseMessaging + cloudDevops + experience + location;
        }

        public int getRole() { return role; }
        public void setRole(int role) { this.role = role; }
        public int getJavaBackend() { return javaBackend; }
        public void setJavaBackend(int javaBackend) { this.javaBackend = javaBackend; }
        public int getSpringMicroservices() { return springMicroservices; }
        public void setSpringMicroservices(int springMicroservices) { this.springMicroservices = springMicroservices; }
        public int getDatabaseMessaging() { return databaseMessaging; }
        public void setDatabaseMessaging(int databaseMessaging) { this.databaseMessaging = databaseMessaging; }
        public int getCloudDevops() { return cloudDevops; }
        public void setCloudDevops(int cloudDevops) { this.cloudDevops = cloudDevops; }
        public int getExperience() { return experience; }
        public void setExperience(int experience) { this.experience = experience; }
        public int getLocation() { return location; }
        public void setLocation(int location) { this.location = location; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExperienceRange {
        private int minimum = 3;
        private int maximum = 7;

        public int getMinimum() { return minimum; }
        public void setMinimum(int minimum) { this.minimum = minimum; }
        public int getMaximum() { return maximum; }
        public void setMaximum(int maximum) { this.maximum = maximum; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Companies {
        private List<String> preferred = new ArrayList<>();
        private List<String> blocked = new ArrayList<>();
        private List<String> spamIndicators = new ArrayList<>();

        public List<String> getPreferred() { return preferred; }
        public void setPreferred(List<String> preferred) { this.preferred = preferred == null ? new ArrayList<>() : preferred; }
        public List<String> getBlocked() { return blocked; }
        public void setBlocked(List<String> blocked) { this.blocked = blocked == null ? new ArrayList<>() : blocked; }
        public List<String> getSpamIndicators() { return spamIndicators; }
        public void setSpamIndicators(List<String> v) { this.spamIndicators = v == null ? new ArrayList<>() : v; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Timeouts {
        private int pageLoad = 45;
        private int explicit = 20;
        private int shortWait = 5;
        private double polling = 0.5;

        public int getPageLoad() { return pageLoad; }
        public void setPageLoad(int pageLoad) { this.pageLoad = pageLoad; }
        public int getExplicit() { return explicit; }
        public void setExplicit(int explicit) { this.explicit = explicit; }
        public int getShort() { return shortWait; }
        public void setShort(int shortWait) { this.shortWait = shortWait; }
        public double getPolling() { return polling; }
        public void setPolling(double polling) { this.polling = polling; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Retry {
        private int maxAttempts = 3;
        private long backoffMillis = 1500;

        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        public long getBackoffMillis() { return backoffMillis; }
        public void setBackoffMillis(long backoffMillis) { this.backoffMillis = backoffMillis; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Paths {
        private String applicationHistory = "data/application-history.json";
        private String screenshots = "screenshots";
        private String reports = "reports";
        private String candidateProfile = "src/main/resources/testdata/candidate-profile.json";
        private String selectors = "src/main/resources/config/selectors.yaml";

        public String getApplicationHistory() { return applicationHistory; }
        public void setApplicationHistory(String v) { this.applicationHistory = v; }
        public String getScreenshots() { return screenshots; }
        public void setScreenshots(String screenshots) { this.screenshots = screenshots; }
        public String getReports() { return reports; }
        public void setReports(String reports) { this.reports = reports; }
        public String getCandidateProfile() { return candidateProfile; }
        public void setCandidateProfile(String candidateProfile) { this.candidateProfile = candidateProfile; }
        public String getSelectors() { return selectors; }
        public void setSelectors(String selectors) { this.selectors = selectors; }
    }

    // ------------------------------------------------------------------ derived helpers

    public RunMode runMode() {
        return RunMode.parse(mode, RunMode.LIVE);
    }

    public String slugFor(String location) {
        String slug = locationSlugs.get(location);
        if (slug != null && !slug.isBlank()) {
            return slug;
        }
        return location.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    // ------------------------------------------------------------------ getters / setters

    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }
    public boolean isHeadless() { return headless; }
    public void setHeadless(boolean headless) { this.headless = headless; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public int getDailyApplicationLimit() { return dailyApplicationLimit; }
    public void setDailyApplicationLimit(int dailyApplicationLimit) { this.dailyApplicationLimit = dailyApplicationLimit; }
    public int getMaxJobsPerSearch() { return maxJobsPerSearch; }
    public void setMaxJobsPerSearch(int maxJobsPerSearch) { this.maxJobsPerSearch = maxJobsPerSearch; }
    public int getMaxSearchPagesPerKeyword() { return maxSearchPagesPerKeyword; }
    public void setMaxSearchPagesPerKeyword(int v) { this.maxSearchPagesPerKeyword = v; }
    public int getMinStrongMatchesPerCity() { return minStrongMatchesPerCity; }
    public void setMinStrongMatchesPerCity(int v) { this.minStrongMatchesPerCity = v; }
    public int getMaxUnknownQuestionsBeforeStop() { return maxUnknownQuestionsBeforeStop; }
    public void setMaxUnknownQuestionsBeforeStop(int v) { this.maxUnknownQuestionsBeforeStop = v; }
    public int getMaxConsecutiveFailuresBeforeStop() { return maxConsecutiveFailuresBeforeStop; }
    public void setMaxConsecutiveFailuresBeforeStop(int v) { this.maxConsecutiveFailuresBeforeStop = v; }
    public int getManualInterventionWaitMinutes() { return manualInterventionWaitMinutes; }
    public void setManualInterventionWaitMinutes(int v) { this.manualInterventionWaitMinutes = v; }
    public Matching getMatching() { return matching; }
    public void setMatching(Matching matching) { this.matching = matching; }
    public int getMinimumSalaryLpa() { return minimumSalaryLpa; }
    public void setMinimumSalaryLpa(int minimumSalaryLpa) { this.minimumSalaryLpa = minimumSalaryLpa; }
    public boolean isRejectUndisclosedSalary() { return rejectUndisclosedSalary; }
    public void setRejectUndisclosedSalary(boolean v) { this.rejectUndisclosedSalary = v; }
    public ExperienceRange getTargetExperience() { return targetExperience; }
    public void setTargetExperience(ExperienceRange targetExperience) { this.targetExperience = targetExperience; }
    public List<String> getLocationPriority() { return locationPriority; }
    public void setLocationPriority(List<String> locationPriority) { this.locationPriority = locationPriority; }
    public Map<String, String> getLocationSlugs() { return locationSlugs; }
    public void setLocationSlugs(Map<String, String> locationSlugs) { this.locationSlugs = locationSlugs == null ? new LinkedHashMap<>() : locationSlugs; }
    public List<String> getSearchKeywords() { return searchKeywords; }
    public void setSearchKeywords(List<String> searchKeywords) { this.searchKeywords = searchKeywords; }
    public List<String> getPreferredRoles() { return preferredRoles; }
    public void setPreferredRoles(List<String> preferredRoles) { this.preferredRoles = preferredRoles; }
    public List<String> getRejectedRoleKeywords() { return rejectedRoleKeywords; }
    public void setRejectedRoleKeywords(List<String> rejectedRoleKeywords) { this.rejectedRoleKeywords = rejectedRoleKeywords; }
    public Companies getCompanies() { return companies; }
    public void setCompanies(Companies companies) { this.companies = companies; }
    public Timeouts getTimeouts() { return timeouts; }
    public void setTimeouts(Timeouts timeouts) { this.timeouts = timeouts; }
    public Retry getRetry() { return retry; }
    public void setRetry(Retry retry) { this.retry = retry; }
    public Paths getPaths() { return paths; }
    public void setPaths(Paths paths) { this.paths = paths; }
}
