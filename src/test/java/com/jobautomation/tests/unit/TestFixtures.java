package com.jobautomation.tests.unit;

import com.jobautomation.config.AppConfig;
import com.jobautomation.config.ConfigLoader;
import com.jobautomation.config.ProfileLoader;
import com.jobautomation.models.CandidateProfile;
import com.jobautomation.models.JobDetails;

import java.util.List;

/** Shared builders for unit tests: the real config.yaml and candidate-profile.json from resources. */
public final class TestFixtures {

    private TestFixtures() {
    }

    public static AppConfig config() {
        return ConfigLoader.load();
    }

    public static CandidateProfile profile() {
        return ProfileLoader.load(config());
    }

    public static JobDetails job(String title, String company, String location, String experience, String salary, List<String> skills, String description) {
        JobDetails d = new JobDetails();
        d.setJobId(String.valueOf(Math.abs((title + company).hashCode())));
        d.setUrl("https://www.naukri.com/job-listings-" + d.getJobId());
        d.setTitle(title);
        d.setCompany(company);
        d.setLocation(location);
        d.setExperienceText(experience);
        d.setSalaryText(salary);
        d.setKeySkills(skills);
        d.setDescription(description);
        d.setEasyApplyAvailable(true);
        return d;
    }

    public static final String STRONG_JAVA_JD = """
            We are looking for a Java Backend Developer with 4-7 years of experience.
            Must have: strong Core Java (Java 11/17), Spring Boot, Microservices, REST APIs.
            Hands-on experience with Kafka, Redis and PostgreSQL / MySQL is required.
            Experience with AWS (EC2, S3), Docker and CI/CD pipelines (Jenkins / GitHub Actions).
            Good understanding of distributed systems, multithreading and concurrency.
            Unit testing with JUnit and Mockito. Fintech / payments domain is a plus.
            """;

    public static final String MANUAL_TESTER_JD = """
            Manual Tester with 3-5 years of experience in functional, regression and UAT testing.
            Must have: test case design, defect tracking with Jira, SQL basics.
            Knowledge of Selenium or Java is a plus but not mandatory.
            """;

    public static final String PYTHON_JD = """
            Senior Python Developer. 5+ years building Django / FastAPI services, Python scripting,
            Python data pipelines, PostgreSQL, Docker. Exposure to Java is nice to have.
            """;
}
