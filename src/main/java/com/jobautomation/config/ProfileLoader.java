package com.jobautomation.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobautomation.exceptions.ConfigurationException;
import com.jobautomation.models.CandidateProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Loads and sanity-checks {@code candidate-profile.json}. */
public final class ProfileLoader {

    private static final Logger log = LoggerFactory.getLogger(ProfileLoader.class);
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private ProfileLoader() {
    }

    public static CandidateProfile load(AppConfig config) {
        String location = config.getPaths().getCandidateProfile();
        try {
            Path p = Paths.get(location);
            CandidateProfile profile;
            if (Files.exists(p)) {
                profile = JSON.readValue(Files.readAllBytes(p), CandidateProfile.class);
            } else {
                // fall back to classpath (testdata/candidate-profile.json)
                String cp = location.replace("src/main/resources/", "");
                try (InputStream in = ProfileLoader.class.getClassLoader().getResourceAsStream(cp)) {
                    if (in == null) {
                        throw new ConfigurationException("Candidate profile not found at " + location + " or classpath:" + cp);
                    }
                    profile = JSON.readValue(in, CandidateProfile.class);
                }
            }
            validate(profile);
            log.info("Candidate profile loaded for {} ({} skills, {} verified skill-year entries)",
                    profile.getName(), profile.getSkills().size(),
                    profile.getSkillExperienceYears().values().stream().filter(v -> v != null).count());
            return profile;
        } catch (IOException e) {
            throw new ConfigurationException("Unable to read candidate profile: " + e.getMessage(), e);
        }
    }

    public static CandidateProfile fromJson(String json) {
        try {
            CandidateProfile profile = JSON.readValue(json, CandidateProfile.class);
            validate(profile);
            return profile;
        } catch (IOException e) {
            throw new ConfigurationException("Invalid candidate profile JSON: " + e.getMessage(), e);
        }
    }

    private static void validate(CandidateProfile p) {
        if (p.getName() == null || p.getName().isBlank()) {
            throw new ConfigurationException("candidate-profile.json: name is required");
        }
        if (p.getSkills().isEmpty()) {
            throw new ConfigurationException("candidate-profile.json: skills must not be empty");
        }
        if (p.getTotalExperienceYears() == null) {
            log.warn("candidate-profile.json: totalExperienceYears is null - any experience question will abort the application");
        }
        if (p.getDob() == null) {
            log.info("candidate-profile.json: dob not configured - applications asking for DOB will be stopped (not guessed)");
        }
    }
}
