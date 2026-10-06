package com.jobautomation.tests.e2e;

import com.jobautomation.config.ProfileLoader;
import com.jobautomation.matcher.JobMatcher;
import com.jobautomation.models.JobDetails;
import com.jobautomation.models.JobListing;
import com.jobautomation.models.MatchResult;
import com.jobautomation.pages.JobDetailsPage;
import com.jobautomation.pages.JobSearchPage;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class JobMatchingTest extends BaseE2ETest {

    @Test(groups = "e2e", description = "A live JD is extracted and scored without opening Easy Apply")
    public void extractsAndScoresLiveJd() {
        List<JobListing> listings = new JobSearchPage(driver).search("Java Spring Boot Developer", "Bengaluru", 1)
                .listings("Bengaluru", "Java Spring Boot Developer");
        Assert.assertFalse(listings.isEmpty());
        JobDetailsPage page = new JobDetailsPage(driver).open(listings.get(0));
        JobDetails details = page.extract(listings.get(0));
        Assert.assertFalse(details.getDescription().isBlank(), "JD text should be extracted");
        MatchResult r = new JobMatcher(config).match(details, ProfileLoader.load(config));
        System.out.println(r.summary());
        Assert.assertTrue(r.getScore() >= 0 && r.getScore() <= 100);
        Assert.assertNotNull(r.getDecision());
    }
}
