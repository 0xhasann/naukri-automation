package com.jobautomation.tests.unit;

import com.jobautomation.config.AppConfig;
import com.jobautomation.config.RunMode;
import com.jobautomation.config.SelectorRepository;
import com.jobautomation.models.CandidateProfile;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ConfigAndProfileTest {

    @Test
    public void configLoadsWithExpectedDefaults() {
        AppConfig c = TestFixtures.config();
        Assert.assertEquals(c.getLocationPriority().get(0), "Bengaluru");
        Assert.assertEquals(c.getLocationPriority().get(1), "Hyderabad");
        Assert.assertEquals(c.getMinimumSalaryLpa(), 20);
        Assert.assertEquals(c.getTargetExperience().getMinimum(), 4);
        Assert.assertEquals(c.getMatching().getWeights().total(), 100);
        Assert.assertTrue(c.getSearchKeywords().contains("Java Developer"));
        Assert.assertTrue(c.getRejectedRoleKeywords().contains("sdet"));
        Assert.assertEquals(c.slugFor("Delhi NCR"), "delhi-ncr");
        Assert.assertEquals(c.slugFor("Unknown City"), "unknown-city");
    }

    @Test
    public void runModeParsing() {
        Assert.assertEquals(RunMode.parse("LIVE", RunMode.DRY_RUN), RunMode.LIVE);
        Assert.assertEquals(RunMode.parse("test_mode", RunMode.DRY_RUN), RunMode.TEST);
        Assert.assertEquals(RunMode.parse("dry-run", RunMode.LIVE), RunMode.DRY_RUN);
        Assert.assertEquals(RunMode.parse(null, RunMode.TEST), RunMode.TEST);
        Assert.assertFalse(RunMode.DRY_RUN.opensEasyApply());
        Assert.assertFalse(RunMode.TEST.submits());
        Assert.assertTrue(RunMode.LIVE.submits());
    }

    @Test
    public void profileIsBuiltFromResumeOnly() {
        CandidateProfile p = TestFixtures.profile();
        Assert.assertEquals(p.getName(), "Mahboob Hasan");
        Assert.assertTrue(p.hasSkill("Java"));
        Assert.assertTrue(p.hasSkill("Spring Boot"));
        Assert.assertTrue(p.hasSkill("Kafka"));
        Assert.assertTrue(p.hasSkill("Redis"));
        Assert.assertTrue(p.hasSkill("PostgreSQL"));
        Assert.assertFalse(p.hasSkill("Hibernate"), "Hibernate is not on the resume");
        Assert.assertFalse(p.hasSkill("Kubernetes"), "Kubernetes is not on the resume");
        Assert.assertFalse(p.hasSkill("Azure"));
        Assert.assertFalse(p.hasSkill("GCP"));
        Assert.assertFalse(p.hasSkill("React"));
        Assert.assertFalse(p.hasSkill("Angular"));
        Assert.assertNotNull(p.getTotalExperienceYears());
        Assert.assertTrue(p.getTotalExperienceYears() < 4.0, "resume shows Nov 2021 - Jul 2025, under 4 years; search target is separate");
    }

    @Test
    public void selectorsResolveWithFallbacks() {
        SelectorRepository s = SelectorRepository.load("src/main/resources/config/selectors.yaml");
        Assert.assertTrue(s.locators("login.usernameField").size() >= 2);
        Assert.assertTrue(s.locators("details.easyApplyButton").size() >= 2);
        Assert.assertTrue(s.locators("security.captcha").size() >= 3);
        Assert.assertEquals(s.rawValues("results.jobId").get(0), "data-job-id");
    }
}
