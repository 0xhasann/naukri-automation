package com.jobautomation.services;

import com.jobautomation.config.AppConfig;
import com.jobautomation.models.JobListing;
import com.jobautomation.pages.JobResultsPage;
import com.jobautomation.pages.JobSearchPage;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * City-by-city, keyword-by-keyword discovery. Keywords rotate per city; listings already seen
 * in this run (by job id) are dropped so overlapping searches do not produce duplicates.
 */
public class JobSearchService {

    private static final Logger log = LoggerFactory.getLogger(JobSearchService.class);

    private final AppConfig config;
    private final WebDriver driver;
    private final Set<String> seenThisRun = new LinkedHashSet<>();

    public JobSearchService(AppConfig config, WebDriver driver) {
        this.config = config;
        this.driver = driver;
    }

    /** Runs one keyword search in one city over the configured number of pages. */
    public List<JobListing> search(String city, String keyword) {
        List<JobListing> out = new ArrayList<>();
        JobSearchPage searchPage = new JobSearchPage(driver);
        for (int page = 1; page <= config.getMaxSearchPagesPerKeyword(); page++) {
            JobResultsPage results = searchPage.search(keyword, city, page);
            List<JobListing> listings = results.listings(city, keyword);
            if (listings.isEmpty()) {
                break;
            }
            int fresh = 0;
            for (JobListing l : listings) {
                if (seenThisRun.add(l.getJobId())) {
                    out.add(l);
                    fresh++;
                }
                if (out.size() >= config.getMaxJobsPerSearch()) {
                    break;
                }
            }
            log.info("City={} keyword='{}' page={} -> {} listings, {} new", city, keyword, page, listings.size(), fresh);
            if (out.size() >= config.getMaxJobsPerSearch() || fresh == 0) {
                break;
            }
        }
        return out;
    }

    public int seenCount() {
        return seenThisRun.size();
    }
}
