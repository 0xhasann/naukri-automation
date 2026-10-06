package com.jobautomation.pages;

import com.jobautomation.models.JobListing;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Search results list. Extracts listings from cards without opening them. */
public class JobResultsPage extends BasePage {

    private static final Pattern ID_IN_URL = Pattern.compile("-(\\d{8,})(?:\\?|$)");

    public JobResultsPage(WebDriver driver) {
        super(driver);
    }

    public boolean hasResults() {
        if (isDisplayed("results.noResults")) {
            return false;
        }
        return waitForVisibleQuietly("results.jobCard", Duration.ofSeconds(config.getTimeouts().getExplicit()));
    }

    /**
     * Reads every job card on the page. A card that cannot be parsed is logged and skipped;
     * it never aborts the page.
     */
    public List<JobListing> listings(String city, String keyword) {
        List<JobListing> out = new ArrayList<>();
        if (!hasResults()) {
            log.info("No results for '{}' in {}", keyword, city);
            return out;
        }
        List<WebElement> cards = findAll("results.jobCard");
        log.info("{} job cards on page", cards.size());
        int index = 0;
        for (WebElement card : cards) {
            index++;
            try {
                JobListing l = parseCard(card);
                if (l == null || l.getJobId() == null || l.getUrl() == null) {
                    log.debug("Card #{} skipped: no job id / url", index);
                    continue;
                }
                l.setSearchCity(city);
                l.setSearchKeyword(keyword);
                out.add(l);
            } catch (StaleElementReferenceException e) {
                log.warn("Card #{} went stale while reading; skipping it", index);
            } catch (RuntimeException e) {
                log.warn("Card #{} could not be parsed ({}); skipping it", index, e.getMessage());
            }
        }
        return out;
    }

    private JobListing parseCard(WebElement card) {
        JobListing l = new JobListing();
        String id = null;
        for (String attr : selectors.rawValues("results.jobId")) {
            String v = safeAttr(card, attr);
            if (!v.isBlank()) {
                id = v;
                break;
            }
        }
        WebElement title = findIn(card, "results.title").orElse(null);
        if (title != null) {
            l.setTitle(safeText(title));
            String href = safeAttr(title, "href");
            if (!href.isBlank()) {
                l.setUrl(href);
                if (id == null) {
                    Matcher m = ID_IN_URL.matcher(href);
                    if (m.find()) {
                        id = m.group(1);
                    }
                }
            }
        }
        l.setJobId(id);
        findIn(card, "results.company").ifPresent(e -> l.setCompany(safeText(e)));
        findIn(card, "results.location").ifPresent(e -> l.setLocation(safeText(e)));
        findIn(card, "results.experience").ifPresent(e -> l.setExperienceText(safeText(e)));
        findIn(card, "results.salary").ifPresent(e -> l.setSalaryText(safeText(e)));
        return l;
    }
}
