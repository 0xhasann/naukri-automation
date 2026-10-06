package com.jobautomation.utils;

import com.jobautomation.exceptions.AutomationStopException;
import com.jobautomation.exceptions.JobSkipException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Retries an action on TRANSIENT Selenium failures (stale element, click intercepted,
 * timeout, not interactable). Stop-the-run and skip-this-job exceptions are never retried.
 */
public final class RetryUtil {

    private static final Logger log = LoggerFactory.getLogger(RetryUtil.class);

    private RetryUtil() {
    }

    public static <T> T withRetry(String description, int maxAttempts, long backoffMillis, Supplier<T> action) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= Math.max(1, maxAttempts); attempt++) {
            try {
                return action.get();
            } catch (AutomationStopException | JobSkipException e) {
                throw e;                                   // never retry deliberate control flow
            } catch (StaleElementReferenceException
                     | ElementNotInteractableException | TimeoutException | NoSuchElementException e) {
                last = e;
                log.warn("{} failed (attempt {}/{}): {}", description, attempt, maxAttempts, firstLine(e.getMessage()));
            } catch (WebDriverException e) {
                // Unknown driver problem: retry once, then give up so the runner can decide
                last = e;
                log.warn("{} raised {} (attempt {}/{}): {}", description, e.getClass().getSimpleName(), attempt, maxAttempts, firstLine(e.getMessage()));
                if (attempt >= 2) {
                    break;
                }
            }
            sleepQuietly(backoffMillis * attempt);
        }
        throw last;
    }

    public static void withRetry(String description, int maxAttempts, long backoffMillis, Runnable action) {
        withRetry(description, maxAttempts, backoffMillis, () -> {
            action.run();
            return null;
        });
    }

    /** Small, bounded pause between retries. This is the ONLY place the framework sleeps. */
    public static void sleepQuietly(long millis) {
        try {
            Thread.sleep(Math.max(0, Math.min(millis, 10_000)));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private static String firstLine(String s) {
        if (s == null) {
            return "";
        }
        int i = s.indexOf('\n');
        return i < 0 ? s : s.substring(0, i);
    }
}
