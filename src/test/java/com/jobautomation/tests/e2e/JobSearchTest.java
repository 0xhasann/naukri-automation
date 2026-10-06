package com.jobautomation.tests.e2e;

import com.jobautomation.models.JobListing;
import com.jobautomation.pages.JobSearchPage;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class JobSearchTest extends BaseE2ETest {

    @Test(groups = "e2e", description = "Search URL is built with keyword, city and experience filter")
    public void buildsSearchUrl() {
        String url = new JobSearchPage(driver).buildUrl("Java Developer", "Bengaluru", 1);
        Assert.assertTrue(url.contains("java-developer-jobs-in-bengaluru"), url);
        Assert.assertTrue(url.contains("experience=" + config.getTargetExperience().getMinimum()), url);
    }

    @Test(groups = "e2e", description = "Bengaluru Java Developer search returns parsable listings")
    public void searchReturnsListings() {
        List<JobListing> listings = new JobSearchPage(driver).search("Java Developer", "Bengaluru", 1).listings("Bengaluru", "Java Developer");
        Assert.assertFalse(listings.isEmpty(), "expected at least one job card");
        JobListing first = listings.get(0);
        Assert.assertNotNull(first.getJobId());
        Assert.assertNotNull(first.getTitle());
        Assert.assertTrue(first.getUrl().startsWith("http"));
    }
}
