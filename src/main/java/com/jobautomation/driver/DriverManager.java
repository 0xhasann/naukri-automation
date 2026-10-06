package com.jobautomation.driver;

import com.jobautomation.config.AppConfig;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds one WebDriver per thread and can detect a dead session.
 * Recovery (re-login, resume from history) is orchestrated by the runner; this class only
 * answers "is the browser still alive?" and restarts it on request.
 */
public final class DriverManager {

    private static final Logger log = LoggerFactory.getLogger(DriverManager.class);
    private static final ThreadLocal<WebDriver> HOLDER = new ThreadLocal<>();

    private DriverManager() {
    }

    public static WebDriver start(AppConfig config) {
        quit();
        WebDriver d = DriverFactory.create(config);
        HOLDER.set(d);
        return d;
    }

    public static WebDriver get() {
        WebDriver d = HOLDER.get();
        if (d == null) {
            throw new IllegalStateException("WebDriver not started for this thread. Call DriverManager.start(config) first.");
        }
        return d;
    }

    public static boolean isStarted() {
        return HOLDER.get() != null;
    }

    /** Cheap liveness probe: a dead ChromeDriver throws on getCurrentUrl(). */
    public static boolean isAlive() {
        WebDriver d = HOLDER.get();
        if (d == null) {
            return false;
        }
        try {
            d.getCurrentUrl();
            d.getWindowHandle();
            return true;
        } catch (WebDriverException e) {
            log.warn("Browser session appears dead: {}", e.getMessage());
            return false;
        }
    }

    public static void quit() {
        WebDriver d = HOLDER.get();
        if (d != null) {
            try {
                d.quit();
            } catch (Exception e) {
                log.debug("Ignoring error while quitting browser: {}", e.getMessage());
            } finally {
                HOLDER.remove();
            }
        }
    }
}
