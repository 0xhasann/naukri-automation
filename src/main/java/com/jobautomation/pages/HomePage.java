package com.jobautomation.pages;

import org.openqa.selenium.WebDriver;

public class HomePage extends BasePage {

    public HomePage(WebDriver driver) {
        super(driver);
    }

    public HomePage open() {
        driver.get(config.getBaseUrl() + "/mnjuser/homepage");
        waitForPageLoad();
        return this;
    }

    /** Logged-in state is inferred from the profile drawer / "My Naukri" links, never from cookies. */
    public boolean isLoggedIn() {
        try {
            if (isDisplayed("home.loggedInIndicator")) {
                return true;
            }
            String url = safeUrl();
            return url.contains("/mnjuser/") && !url.contains("nlogin");
        } catch (RuntimeException e) {
            return false;
        }
    }

    public JobSearchPage jobSearch() {
        return new JobSearchPage(driver);
    }
}
