package com.jobautomation.tests.unit;

import com.jobautomation.config.AppConfig;
import com.jobautomation.matcher.JobMatcher;
import com.jobautomation.models.CandidateProfile;
import com.jobautomation.models.Decision;
import com.jobautomation.models.JobDetails;
import com.jobautomation.models.MatchResult;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

public class JobMatcherTest {

    private AppConfig config;
    private CandidateProfile profile;
    private JobMatcher matcher;

    @BeforeClass
    public void setUp() {
        config = TestFixtures.config();
        profile = TestFixtures.profile();
        matcher = new JobMatcher(config);
    }

    @Test(description = "Java + Spring Boot + Kafka + Redis in Bengaluru is a strong match")
    public void strongJavaBackendJobIsEligible() {
        JobDetails job = TestFixtures.job("Java Backend Developer", "ABC Fintech", "Bengaluru", "4-7 Yrs", "20-30 Lacs PA",
                List.of("Java", "Spring Boot", "Microservices", "Kafka", "Redis", "PostgreSQL", "AWS"), TestFixtures.STRONG_JAVA_JD);
        MatchResult r = matcher.match(job, profile);
        System.out.println(r.summary());
        Assert.assertEquals(r.getDecision(), Decision.ELIGIBLE, r.summary());
        Assert.assertTrue(r.getScore() >= config.getMatching().getEligibleScore(), "score " + r.getScore());
        Assert.assertTrue(r.getMatchedSkills().contains("java"));
        Assert.assertTrue(r.getMatchedSkills().contains("spring boot"));
        Assert.assertTrue(r.getMatchedSkills().contains("kafka"));
        Assert.assertTrue(r.getMatchedSkills().contains("redis"));
        Assert.assertFalse(r.isHardReject());
    }

    @Test(description = "A Manual Tester JD is rejected outright")
    public void manualTesterIsRejected() {
        JobDetails job = TestFixtures.job("Manual Tester", "QA Corp", "Bengaluru", "3-5 Yrs", "Not Disclosed",
                List.of("Manual Testing", "Jira"), TestFixtures.MANUAL_TESTER_JD);
        MatchResult r = matcher.match(job, profile);
        Assert.assertEquals(r.getDecision(), Decision.REJECT);
        Assert.assertTrue(r.isHardReject());
        Assert.assertEquals(r.getScore(), 0);
    }

    @Test(description = "SDET / QA automation titles are rejected even when Java is in the JD")
    public void sdetIsRejected() {
        JobDetails job = TestFixtures.job("SDET - Java Selenium", "Testing Co", "Pune", "4-8 Yrs", "22-28 Lacs PA",
                List.of("Java", "Selenium", "TestNG"), "SDET with Java, Selenium, TestNG, REST Assured, CI/CD.");
        Assert.assertEquals(matcher.match(job, profile).getDecision(), Decision.REJECT);
    }

    @Test(description = "Python role where Java is only 'nice to have' is rejected")
    public void pythonRoleIsRejected() {
        JobDetails job = TestFixtures.job("Senior Python Developer", "Data Co", "Bengaluru", "5-9 Yrs", "25-40 Lacs PA",
                List.of("Python", "Django", "PostgreSQL"), TestFixtures.PYTHON_JD);
        MatchResult r = matcher.match(job, profile);
        Assert.assertEquals(r.getDecision(), Decision.REJECT, r.summary());
        Assert.assertTrue(r.isHardReject());
    }

    @Test(description = "Backend role where Java appears only as optional is rejected")
    public void javaOptionalOnlyIsRejected() {
        JobDetails job = TestFixtures.job("Backend Engineer", "Startup", "Bengaluru", "3-6 Yrs", "Not Disclosed",
                List.of("Node.js", "TypeScript", "MongoDB"),
                "Backend engineer building Node.js services with TypeScript, MongoDB and AWS. Good to have: Java or Go.");
        MatchResult r = matcher.match(job, profile);
        Assert.assertEquals(r.getDecision(), Decision.REJECT, r.summary());
    }

    @Test(description = "Disclosed salary below the minimum rejects the job even when skills match")
    public void lowSalaryIsRejected() {
        JobDetails job = TestFixtures.job("Java Developer", "Budget Ltd", "Bengaluru", "4-6 Yrs", "8-12 Lacs PA",
                List.of("Java", "Spring Boot", "Kafka"), TestFixtures.STRONG_JAVA_JD);
        MatchResult r = matcher.match(job, profile);
        Assert.assertEquals(r.getDecision(), Decision.REJECT);
        Assert.assertTrue(r.getReasons().get(0).contains("below minimum"), r.getReasons().toString());
    }

    @Test(description = "Undisclosed salary does not reject a strong match")
    public void undisclosedSalaryIsAllowed() {
        JobDetails job = TestFixtures.job("Java Developer", "Product Co", "Bengaluru", "4-6 Yrs", "Not Disclosed",
                List.of("Java", "Spring Boot", "Kafka", "Redis", "PostgreSQL", "AWS"), TestFixtures.STRONG_JAVA_JD);
        Assert.assertEquals(matcher.match(job, profile).getDecision(), Decision.ELIGIBLE);
    }

    @Test(description = "Experience far above the verified profile rejects the job")
    public void seniorExperienceIsRejected() {
        JobDetails job = TestFixtures.job("Senior Java Developer", "Big Co", "Bengaluru", "8-12 Yrs", "35-50 Lacs PA",
                List.of("Java", "Spring Boot", "Kafka"), TestFixtures.STRONG_JAVA_JD.replace("4-7 years", "8-12 years"));
        MatchResult r = matcher.match(job, profile);
        Assert.assertEquals(r.getDecision(), Decision.REJECT, r.summary());
        Assert.assertTrue(r.isHardReject());
    }

    @Test(description = "Blocked companies are never eligible")
    public void blockedCompanyIsRejected() {
        AppConfig c = TestFixtures.config();
        c.getCompanies().setBlocked(List.of("Blocked Inc"));
        JobDetails job = TestFixtures.job("Java Developer", "Blocked Inc", "Bengaluru", "4-6 Yrs", "25-30 Lacs PA",
                List.of("Java", "Spring Boot"), TestFixtures.STRONG_JAVA_JD);
        Assert.assertEquals(new JobMatcher(c).match(job, profile).getDecision(), Decision.REJECT);
    }

    @Test(description = "Missing skills are reported, not invented")
    public void missingSkillsAreReported() {
        JobDetails job = TestFixtures.job("Java Developer", "K8s Co", "Hyderabad", "4-6 Yrs", "Not Disclosed",
                List.of("Java", "Spring Boot", "Kubernetes", "Azure", "Hibernate"),
                "Java, Spring Boot, Hibernate/JPA, Kubernetes on Azure, Microservices, REST.");
        MatchResult r = matcher.match(job, profile);
        Assert.assertTrue(r.getMissingSkills().contains("kubernetes"), r.getMissingSkills().toString());
        Assert.assertTrue(r.getMissingSkills().contains("azure"), r.getMissingSkills().toString());
        Assert.assertTrue(r.getMissingSkills().contains("hibernate"), r.getMissingSkills().toString());
        Assert.assertFalse(r.getMatchedSkills().contains("kubernetes"));
    }

    @Test(description = "Score is never exposed above 100 and weights normalise")
    public void scoreBounded() {
        JobDetails job = TestFixtures.job("Java Developer", "X", "Bengaluru", "4-6 Yrs", "25-30 Lacs PA",
                List.of("Java", "Spring Boot", "Microservices", "Kafka", "Redis", "PostgreSQL", "MySQL", "AWS", "Docker", "Jenkins"),
                TestFixtures.STRONG_JAVA_JD);
        MatchResult r = matcher.match(job, profile);
        Assert.assertTrue(r.getScore() >= 0 && r.getScore() <= 100);
        int sum = r.getMaxPoints().values().stream().mapToInt(Integer::intValue).sum();
        Assert.assertEquals(sum, 100, "weights must total 100");
    }

    @Test(description = "JD demanding an immediate joiner is rejected when the profile is not immediately available")
    public void immediateJoinerConstraintIsRespected() {
        JobDetails job = TestFixtures.job("Java Developer", "Fast Co", "Bengaluru", "4-6 Yrs", "22-30 Lacs PA",
                List.of("Java", "Spring Boot", "Kafka", "Redis"), TestFixtures.STRONG_JAVA_JD + " Only immediate joiners will be considered.");
        Assert.assertEquals(matcher.match(job, profile).getDecision(), Decision.ELIGIBLE, "profile is an immediate joiner");
        CandidateProfile notImmediate = TestFixtures.profile();
        notImmediate.setImmediateJoiner(false);
        notImmediate.setNoticePeriodDays(60);
        MatchResult r = matcher.match(job, notImmediate);
        Assert.assertEquals(r.getDecision(), Decision.REJECT, r.summary());
        Assert.assertTrue(r.isHardReject());
    }
}
