package com.jobautomation.tests.e2e;

import com.jobautomation.config.AppConfig;
import com.jobautomation.config.ConfigLoader;
import com.jobautomation.config.Credentials;
import com.jobautomation.driver.DriverManager;
import com.jobautomation.exceptions.MissingCredentialsException;
import com.jobautomation.pages.HomePage;
import com.jobautomation.pages.LoginPage;
import org.openqa.selenium.WebDriver;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * Browser tests share one logged-in session per class. They are skipped (not failed) when
 * NAUKRI_USERNAME / NAUKRI_PASSWORD are not exported, so {@code mvn test} stays green on CI.
 */
public abstract class BaseE2ETest {

    protected AppConfig config;
    protected WebDriver driver;
    protected HomePage home;

    @BeforeClass(alwaysRun = true)
    public void startSession() {
        config = ConfigLoader.get();
        Credentials credentials;
        try {
            credentials = Credentials.fromEnvironment();
        } catch (MissingCredentialsException e) {
            throw new SkipException("Skipping browser test: " + e.getMessage());
        }
        driver = DriverManager.start(config);
        home = new LoginPage(driver).open().login(credentials);
    }

    @AfterClass(alwaysRun = true)
    public void stopSession() {
        DriverManager.quit();
    }
}
