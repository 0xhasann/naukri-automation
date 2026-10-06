package com.jobautomation.tests.unit;

import com.jobautomation.matcher.SkillMatcher;
import com.jobautomation.matcher.SkillTaxonomy;
import com.jobautomation.matcher.TextNormalizer;
import com.jobautomation.models.CandidateProfile;
import com.jobautomation.models.JobDetails;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

public class SkillMatcherTest {

    private final CandidateProfile profile = TestFixtures.profile();
    private final SkillMatcher matcher = new SkillMatcher();

    @Test
    public void synonymsResolveToCanonicalSkills() {
        Assert.assertEquals(SkillTaxonomy.canonical("SpringBoot"), "spring boot");
        Assert.assertEquals(SkillTaxonomy.canonical("Spring-Boot"), "spring boot");
        Assert.assertEquals(SkillTaxonomy.canonical("Apache Kafka"), "kafka");
        Assert.assertEquals(SkillTaxonomy.canonical("Postgres"), "postgresql");
        Assert.assertEquals(SkillTaxonomy.canonical("Amazon Web Services"), "aws");
        Assert.assertEquals(SkillTaxonomy.canonical("Java 17"), "java");
        Assert.assertEquals(SkillTaxonomy.canonical("multi-threading"), "concurrency");
    }

    @Test
    public void javaDoesNotMatchJavaScript() {
        Set<String> found = SkillTaxonomy.findSkills("Frontend developer with JavaScript, React and CSS");
        Assert.assertFalse(found.contains("java"), found.toString());
        Assert.assertTrue(found.contains("javascript"));
        Assert.assertTrue(found.contains("react"));
    }

    @Test
    public void equivalentJdWordingIsRecognised() {
        Set<String> found = SkillTaxonomy.findSkills(
                "Java server-side development with Spring Boot microservices, event-driven architecture, "
                        + "high-throughput systems and Kafka-based pipelines");
        Assert.assertTrue(found.contains("java"));
        Assert.assertTrue(found.contains("backend development"));
        Assert.assertTrue(found.contains("spring boot"));
        Assert.assertTrue(found.contains("microservices"));
        Assert.assertTrue(found.contains("event-driven architecture"));
        Assert.assertTrue(found.contains("distributed systems"));
        Assert.assertTrue(found.contains("kafka"));
    }

    @Test
    public void profileHasOnlyResumeSkills() {
        Assert.assertTrue(profile.hasSkill("Java"));
        Assert.assertTrue(profile.hasSkill("Spring Boot"));
        Assert.assertTrue(profile.hasSkill("Kafka"));
        Assert.assertTrue(profile.hasSkill("AWS"));
        Assert.assertFalse(profile.hasSkill("Kubernetes"));
        Assert.assertFalse(profile.hasSkill("Hibernate"));
        Assert.assertFalse(profile.hasSkill("Azure"));
        Assert.assertFalse(profile.hasSkill("React"));
        Assert.assertFalse(profile.hasSkill("Angular"));
    }

    @Test
    public void categoryScoresSeparateMatchedAndMissing() {
        JobDetails job = TestFixtures.job("Java Developer", "X", "Pune", "4-6 Yrs", null,
                List.of("Java", "Spring Boot", "Kafka", "Kubernetes"), "Java, Spring Boot, Kafka, Kubernetes, GCP");
        Set<String> jd = matcher.requiredSkills(job);
        SkillMatcher.CategoryScore cloud = matcher.score(SkillTaxonomy.Category.CLOUD_DEVOPS, jd, profile);
        Assert.assertTrue(cloud.missing.contains("kubernetes"));
        Assert.assertTrue(cloud.missing.contains("gcp"));
        Assert.assertTrue(cloud.fraction < 0.5);
        SkillMatcher.CategoryScore data = matcher.score(SkillTaxonomy.Category.DATABASE_MESSAGING, jd, profile);
        Assert.assertEquals(data.matched, List.of("kafka"));
        Assert.assertEquals(data.fraction, 1.0, 0.001);
    }

    @Test
    public void jdSilentOnCategoryIsNeutral() {
        JobDetails job = TestFixtures.job("Java Developer", "X", "Pune", "4-6 Yrs", null, List.of("Java"), "Core Java developer.");
        Set<String> jd = matcher.requiredSkills(job);
        SkillMatcher.CategoryScore cloud = matcher.score(SkillTaxonomy.Category.CLOUD_DEVOPS, jd, profile);
        Assert.assertFalse(cloud.jdMentionsCategory);
        Assert.assertEquals(cloud.fraction, 0.5, 0.001);
    }

    @Test
    public void comboBonusRewardsHighValueCombinations() {
        Set<String> jd = Set.of("java", "spring boot", "kafka", "redis", "aws");
        Assert.assertEquals(matcher.comboBonus(jd, profile), 1.0, 0.001);
        Assert.assertEquals(matcher.comboBonus(Set.of("java"), profile), 0.0, 0.001);
    }

    @Test
    public void normalizerStripsHtmlAndPunctuation() {
        String n = TextNormalizer.normalize("<p>Java/Spring&nbsp;Boot, Kafka; Redis (mandatory)</p>");
        Assert.assertEquals(n, "java spring boot kafka redis mandatory");
    }
}
