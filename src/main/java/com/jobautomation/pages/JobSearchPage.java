package com.jobautomation.pages;

import com.jobautomation.components.SecurityChallengeDetector;
import org.openqa.selenium.WebDriver;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Naukri search is driven by URL, which is far more stable than the type-ahead search box:
 * <pre>https://www.naukri.com/java-developer-jobs-in-bengaluru-2?k=java%20developer&l=bengaluru&experience=4</pre>
 * Keyword, location and experience are applied this way; salary is filtered by the matcher
 * because Naukri's CTC buckets (15-25, 25-50 ...) do not line up with a "20 LPA minimum".
 */
public class JobSearchPage extends BasePage {

    private final SecurityChallengeDetector security;

    public JobSearchPage(WebDriver driver) {
        super(driver);
        this.security = new SecurityChallengeDetector(driver);
    }

    public String buildUrl(String keyword, String city, int page) {
        String kSlug = slug(keyword);
        String lSlug = config.slugFor(city);
        StringBuilder url = new StringBuilder(config.getBaseUrl())
                .append('/').append(kSlug).append("-jobs-in-").append(lSlug);
        if (page > 1) {
            url.append('-').append(page);
        }
        url.append("?k=").append(URLEncoder.encode(keyword, StandardCharsets.UTF_8))
                .append("&l=").append(URLEncoder.encode(city, StandardCharsets.UTF_8))
                .append("&experience=").append(config.getTargetExperience().getMinimum());
        return url.toString();
    }

    public JobResultsPage search(String keyword, String city, int page) {
        String url = buildUrl(keyword, city, page);
        log.info("Search: keyword='{}' city='{}' page={} -> {}", keyword, city, page, url);
        driver.get(url);
        waitForPageLoad();
        security.assertNoChallenge("job search");
        return new JobResultsPage(driver);
    }

    static String slug(String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
