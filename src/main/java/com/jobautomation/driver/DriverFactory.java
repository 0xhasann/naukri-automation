package com.jobautomation.driver;

import com.jobautomation.config.AppConfig;
import com.jobautomation.exceptions.AutomationStopException;
import com.jobautomation.exceptions.StopReason;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Locale;

/**
 * Creates a plain, unmodified browser session.
 * <p>
 * Deliberately NO stealth / fingerprint manipulation / automation-flag hiding: this framework
 * behaves as an ordinary logged-in user and stops when the site asks for human verification.
 */
public final class DriverFactory {

    private static final Logger log = LoggerFactory.getLogger(DriverFactory.class);

    private DriverFactory() {
    }

    public static WebDriver create(AppConfig config) {
        String browser = config.getBrowser() == null ? "chrome" : config.getBrowser().toLowerCase(Locale.ROOT);
        WebDriver driver;
        try {
            switch (browser) {
                case "firefox" -> {
                    WebDriverManager.firefoxdriver().setup();
                    FirefoxOptions o = new FirefoxOptions();
                    if (config.isHeadless()) {
                        o.addArguments("-headless");
                    }
                    o.setPageLoadStrategy(PageLoadStrategy.NORMAL);
                    driver = new FirefoxDriver(o);
                }
                case "edge" -> {
                    WebDriverManager.edgedriver().setup();
                    EdgeOptions o = new EdgeOptions();
                    if (config.isHeadless()) {
                        o.addArguments("--headless=new");
                    }
                    o.addArguments("--window-size=1400,1000", "--disable-notifications");
                    driver = new EdgeDriver(o);
                }
                default -> {
                    WebDriverManager.chromedriver().setup();
                    ChromeOptions o = new ChromeOptions();
                    if (config.isHeadless()) {
                        o.addArguments("--headless=new");
                    }
                    o.addArguments(
                            "--window-size=1400,1000",
                            "--disable-notifications",
                            "--disable-popup-blocking",
                            "--no-first-run",
                            "--no-default-browser-check",
                            "--disable-dev-shm-usage",
                            "--no-sandbox",
                            "--remote-allow-origins=*");
                    String binary = System.getProperty("chrome.binary");
                    if (binary != null && !binary.isBlank()) {
                        o.setBinary(binary);          // -Dchrome.binary=/path/to/chrome for non-standard installs
                    }
                    o.setPageLoadStrategy(PageLoadStrategy.NORMAL);
                    driver = new ChromeDriver(o);
                }
            }
        } catch (Exception e) {
            throw new AutomationStopException(StopReason.BROWSER_UNSTABLE,
                    "Unable to start browser '" + browser + "': " + e.getMessage(), e);
        }
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(config.getTimeouts().getPageLoad()));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(config.getTimeouts().getExplicit()));
        // Implicit wait intentionally 0: all synchronisation is done with explicit waits in BasePage.
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        if (!config.isHeadless()) {
            try {
                driver.manage().window().maximize();
            } catch (Exception ignored) {
                // some window managers refuse; not fatal
            }
        }
        log.info("Browser started: {} (headless={})", browser, config.isHeadless());
        return driver;
    }
}
