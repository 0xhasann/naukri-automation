package com.jobautomation.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jobautomation.exceptions.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Loads {@code config.yaml} (classpath or file) and applies JVM system-property overrides
 * so any value can be changed from the command line without editing the file:
 * <pre>
 *   mvn test -Dsuite=testng-e2e.xml -Dmode=LIVE -DdailyApplicationLimit=10 -Dheadless=true
 * </pre>
 */
public final class ConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);
    private static final String DEFAULT_CLASSPATH_LOCATION = "config/config.yaml";
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static volatile AppConfig cached;

    private ConfigLoader() {
    }

    /** Cached singleton; loaded once per JVM. */
    public static AppConfig get() {
        AppConfig c = cached;
        if (c == null) {
            synchronized (ConfigLoader.class) {
                c = cached;
                if (c == null) {
                    c = load();
                    cached = c;
                }
            }
        }
        return c;
    }

    /** Force a reload (used by tests). */
    public static synchronized AppConfig reload() {
        cached = load();
        return cached;
    }

    public static AppConfig load() {
        String override = System.getProperty("config");
        AppConfig config;
        try {
            if (override != null && !override.isBlank()) {
                Path p = Paths.get(override);
                log.info("Loading configuration from file {}", p.toAbsolutePath());
                config = YAML.readValue(Files.readAllBytes(p), AppConfig.class);
            } else {
                try (InputStream in = ConfigLoader.class.getClassLoader().getResourceAsStream(DEFAULT_CLASSPATH_LOCATION)) {
                    if (in == null) {
                        throw new ConfigurationException("config/config.yaml not found on the classpath");
                    }
                    config = YAML.readValue(in, AppConfig.class);
                }
            }
        } catch (IOException e) {
            throw new ConfigurationException("Unable to read configuration: " + e.getMessage(), e);
        }
        applySystemPropertyOverrides(config);
        validate(config);
        return config;
    }

    private static void applySystemPropertyOverrides(AppConfig c) {
        String mode = System.getProperty("mode");
        if (mode != null && !mode.isBlank()) {
            c.setMode(mode);
        }
        String headless = System.getProperty("headless");
        if (headless != null) {
            c.setHeadless(Boolean.parseBoolean(headless));
        }
        String browser = System.getProperty("browser");
        if (browser != null && !browser.isBlank()) {
            c.setBrowser(browser);
        }
        String limit = System.getProperty("dailyApplicationLimit");
        if (limit != null && !limit.isBlank()) {
            c.setDailyApplicationLimit(Integer.parseInt(limit.trim()));
        }
        String minSalary = System.getProperty("minimumSalaryLpa");
        if (minSalary != null && !minSalary.isBlank()) {
            c.setMinimumSalaryLpa(Integer.parseInt(minSalary.trim()));
        }
        String minScore = System.getProperty("minimumMatchScore");
        if (minScore != null && !minScore.isBlank()) {
            c.getMatching().setEligibleScore(Integer.parseInt(minScore.trim()));
        }
        String maxJobs = System.getProperty("maxJobsPerSearch");
        if (maxJobs != null && !maxJobs.isBlank()) {
            c.setMaxJobsPerSearch(Integer.parseInt(maxJobs.trim()));
        }
        String pages = System.getProperty("maxSearchPagesPerKeyword");
        if (pages != null && !pages.isBlank()) {
            c.setMaxSearchPagesPerKeyword(Integer.parseInt(pages.trim()));
        }
        String wait = System.getProperty("manualInterventionWaitMinutes");
        if (wait != null && !wait.isBlank()) {
            c.setManualInterventionWaitMinutes(Integer.parseInt(wait.trim()));
        }
        String profile = System.getProperty("candidateProfile");
        if (profile != null && !profile.isBlank()) {
            c.getPaths().setCandidateProfile(profile);
        }
        String history = System.getProperty("applicationHistory");
        if (history != null && !history.isBlank()) {
            c.getPaths().setApplicationHistory(history);
        }
    }

    private static void validate(AppConfig c) {
        RunMode.parse(c.getMode(), RunMode.DRY_RUN); // throws IllegalArgumentException on garbage
        if (c.getDailyApplicationLimit() < 0) {
            throw new ConfigurationException("dailyApplicationLimit must be >= 0");
        }
        if (c.getMatching().getEligibleScore() < c.getMatching().getReviewScore()) {
            throw new ConfigurationException("matching.eligibleScore must be >= matching.reviewScore");
        }
        int total = c.getMatching().getWeights().total();
        if (total != 100) {
            log.warn("Matching weights add up to {} (expected 100). Scores will be normalised.", total);
        }
        if (c.getLocationPriority() == null || c.getLocationPriority().isEmpty()) {
            throw new ConfigurationException("locationPriority must contain at least one location");
        }
        if (c.getSearchKeywords() == null || c.getSearchKeywords().isEmpty()) {
            throw new ConfigurationException("searchKeywords must contain at least one keyword");
        }
    }
}
