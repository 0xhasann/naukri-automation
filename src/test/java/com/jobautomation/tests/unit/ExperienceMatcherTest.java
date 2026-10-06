package com.jobautomation.tests.unit;

import com.jobautomation.matcher.ExperienceMatcher;
import com.jobautomation.matcher.JdParser;
import com.jobautomation.models.CandidateProfile;
import com.jobautomation.models.JobDetails;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ExperienceMatcherTest {

    private final ExperienceMatcher matcher = new ExperienceMatcher();

    private static CandidateProfile candidate(Double years) {
        CandidateProfile p = new CandidateProfile();
        p.setName("t");
        p.setSkills(java.util.List.of("Java"));
        p.setTotalExperienceYears(years);
        return p;
    }

    private static JobDetails jobWithExperience(String text) {
        JobDetails d = new JobDetails();
        d.setExperienceText(text);
        d.setDescription("");
        JdParser.enrich(d);
        return d;
    }

    @Test
    public void parsesRanges() {
        Assert.assertEquals(JdParser.parseExperienceRange("4 - 7 years"), new int[]{4, 7});
        Assert.assertEquals(JdParser.parseExperienceRange("4-7 Yrs"), new int[]{4, 7});
        Assert.assertEquals(JdParser.parseExperienceRange("3 to 6 yrs"), new int[]{3, 6});
        Assert.assertEquals(JdParser.parseExperienceRange("5+ years"), new int[]{5, -1});
        Assert.assertEquals(JdParser.parseExperienceRange("Minimum 4 years of experience"), new int[]{4, -1});
        Assert.assertNull(JdParser.parseExperienceRange("Not mentioned"));
    }

    @Test
    public void withinRangeIsFullScore() {
        ExperienceMatcher.Result r = matcher.evaluate(jobWithExperience("3-6 Yrs"), candidate(3.7));
        Assert.assertEquals(r.score, 1.0, 0.001);
        Assert.assertFalse(r.hardReject);
    }

    @Test
    public void slightlyBelowMinimumIsToleratedNotFaked() {
        ExperienceMatcher.Result r = matcher.evaluate(jobWithExperience("4-7 Yrs"), candidate(3.7));
        Assert.assertFalse(r.hardReject);
        Assert.assertTrue(r.score >= 0.7 && r.score < 1.0, "score " + r.score);
    }

    @Test
    public void farAboveVerifiedExperienceRejects() {
        ExperienceMatcher.Result r = matcher.evaluate(jobWithExperience("8-12 Yrs"), candidate(3.7));
        Assert.assertTrue(r.hardReject);
    }

    @Test
    public void unknownCandidateExperienceIsNeutral() {
        ExperienceMatcher.Result r = matcher.evaluate(jobWithExperience("4-7 Yrs"), candidate(null));
        Assert.assertFalse(r.hardReject);
        Assert.assertEquals(r.score, 0.5, 0.001);
    }

    @Test
    public void jdWithoutRangeIsNeutral() {
        ExperienceMatcher.Result r = matcher.evaluate(jobWithExperience(""), candidate(3.7));
        Assert.assertEquals(r.score, 0.7, 0.001);
    }
}
