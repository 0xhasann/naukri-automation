package com.jobautomation.pages;

import com.jobautomation.components.SecurityChallengeDetector;
import com.jobautomation.config.Credentials;
import com.jobautomation.exceptions.LoginFailedException;
import com.jobautomation.exceptions.SelectorNotFoundException;
import com.jobautomation.utils.ScreenshotUtil;
import org.openqa.selenium.WebDriver;

import java.time.Duration;

public class LoginPage extends BasePage {

    public static final String LOGIN_PATH = "/nlogin/login";

    private final SecurityChallengeDetector security;

    public LoginPage(WebDriver driver) {
        super(driver);
        this.security = new SecurityChallengeDetector(driver);
    }

    public LoginPage open() {
        driver.get(config.getBaseUrl() + LOGIN_PATH);
        waitForPageLoad();
        security.assertNoChallenge("login page load");
        return this;
    }

    /**
     * Logs in with credentials from the environment. Fails the whole run (never retries with
     * guessed credentials) on error, and stops if an OTP / CAPTCHA appears.
     */
    public HomePage login(Credentials credentials) {
        log.info("Logging in as {}", credentials.maskedUsername());
        try {
            type("login.usernameField", credentials.username());
            type("login.passwordField", credentials.password());
            click("login.submitButton");
        } catch (SelectorNotFoundException e) {
            String shot = ScreenshotUtil.capture(driver, "login_form_missing");
            throw new LoginFailedException("Login form not found (" + e.getMessage() + "). Screenshot: " + shot, e);
        }

        HomePage home = new HomePage(driver);
        boolean loggedIn = false;
        try {
            loggedIn = fluentWait(Duration.ofSeconds(config.getTimeouts().getExplicit())).until(d -> {
                if (security.detect() != null) {
                    return true;            // stop waiting: a challenge appeared
                }
                if (isDisplayed("login.errorMessage")) {
                    return true;            // stop waiting: an error is shown
                }
                return home.isLoggedIn();
            });
        } catch (org.openqa.selenium.TimeoutException ignored) {
            // handled below
        }

        security.assertNoChallenge("login");                 // OTP / CAPTCHA -> stop, never bypass
        if (isDisplayed("login.errorMessage")) {
            String msg = getText("login.errorMessage");
            String shot = ScreenshotUtil.capture(driver, "login_error");
            throw new LoginFailedException("Naukri rejected the login: '" + msg + "'. Screenshot: " + shot);
        }
        if (!loggedIn || !home.isLoggedIn()) {
            String shot = ScreenshotUtil.capture(driver, "login_not_confirmed");
            throw new LoginFailedException("Login could not be confirmed (no profile menu after submit). Screenshot: " + shot);
        }
        log.info("Login successful");
        return home;
    }
}
