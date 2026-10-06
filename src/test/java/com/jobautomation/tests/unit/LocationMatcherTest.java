package com.jobautomation.tests.unit;

import com.jobautomation.matcher.LocationMatcher;
import com.jobautomation.models.CandidateProfile;
import com.jobautomation.models.JobDetails;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LocationMatcherTest {

    private final LocationMatcher matcher = new LocationMatcher(TestFixtures.config());
    private final CandidateProfile profile = TestFixtures.profile();

    private static JobDetails at(String location, String mode) {
        JobDetails d = new JobDetails();
        d.setLocation(location);
        d.setWorkMode(mode);
        return d;
    }

    @Test
    public void bengaluruIsTopPriority() {
        Assert.assertEquals(matcher.evaluate(at("Bengaluru", null), profile).score, 1.0, 0.001);
        Assert.assertEquals(matcher.evaluate(at("Bangalore/Bengaluru", null), profile).score, 1.0, 0.001);
    }

    @Test
    public void lowerPriorityCitiesScoreLower() {
        double hyd = matcher.evaluate(at("Hyderabad", null), profile).score;
        double ncr = matcher.evaluate(at("Noida", null), profile).score;
        double pune = matcher.evaluate(at("Pune", null), profile).score;
        double chennai = matcher.evaluate(at("Chennai", null), profile).score;
        Assert.assertTrue(hyd < 1.0 && hyd >= ncr && ncr >= pune && pune >= chennai, hyd + " " + ncr + " " + pune + " " + chennai);
        Assert.assertTrue(chennai >= 0.7);
    }

    @Test
    public void remoteIsFullScore() {
        Assert.assertEquals(matcher.evaluate(at("Remote", null), profile).score, 1.0, 0.001);
        Assert.assertEquals(matcher.evaluate(at("Kolkata", "Remote"), profile).score, 1.0, 0.001);
    }

    @Test
    public void otherCityWithRelocationIsPartial() {
        LocationMatcher.Result r = matcher.evaluate(at("Kochi", null), profile);
        Assert.assertTrue(r.score > 0 && r.score < 0.7, "score " + r.score);
    }

    @Test
    public void aliasesMatch() {
        Assert.assertTrue(LocationMatcher.matches("gurgaon, haryana", "Gurugram"));
        Assert.assertTrue(LocationMatcher.matches("noida", "Delhi NCR"));
        Assert.assertFalse(LocationMatcher.matches("chennai", "Bengaluru"));
    }
}
