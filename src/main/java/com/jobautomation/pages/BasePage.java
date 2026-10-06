package com.jobautomation.pages;

import com.jobautomation.config.AppConfig;
import com.jobautomation.config.ConfigLoader;
import com.jobautomation.config.SelectorRepository;
import com.jobautomation.exceptions.SelectorNotFoundException;
import com.jobautomation.utils.RetryUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Common Selenium plumbing: fallback selector resolution, explicit/fluent waits, safe
 * click/type/getText. No Thread.sleep-based synchronisation.
 */
public abstract class BasePage {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final WebDriver driver;
    protected final AppConfig config;
    protected final SelectorRepository selectors;
    private final Duration explicitTimeout;
    private final Duration shortTimeout;
    private final Duration polling;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.config = ConfigLoader.get();
        this.selectors = SelectorRepository.get();
        this.explicitTimeout = Duration.ofSeconds(config.getTimeouts().getExplicit());
        this.shortTimeout = Duration.ofSeconds(config.getTimeouts().getShort());
        this.polling = Duration.ofMillis((long) (config.getTimeouts().getPolling() * 1000));
    }

    // ------------------------------------------------------------------ selector resolution

    /** First fallback locator under {@code key} that currently resolves to at least one element. */
    protected Optional<By> resolve(String key) {
        for (By by : selectors.locators(key)) {
            try {
                if (!driver.findElements(by).isEmpty()) {
                    return Optional.of(by);
                }
            } catch (RuntimeException e) {
                log.debug("Locator {} for {} threw {}", by, key, e.getClass().getSimpleName());
            }
        }
        return Optional.empty();
    }

    /** Resolves within a scope element (e.g. a job card). */
    protected Optional<WebElement> findIn(WebElement scope, String key) {
        for (By by : selectors.locators(key)) {
            try {
                List<WebElement> found = scope.findElements(by);
                if (!found.isEmpty()) {
                    return Optional.of(found.get(0));
                }
            } catch (StaleElementReferenceException e) {
                throw e;
            } catch (RuntimeException ignored) {
                // try next fallback
            }
        }
        return Optional.empty();
    }

    protected Optional<WebElement> find(String key) {
        return resolve(key).map(by -> driver.findElements(by)).filter(l -> !l.isEmpty()).map(l -> l.get(0));
    }

    protected List<WebElement> findAll(String key) {
        return resolve(key).map(by -> driver.findElements(by)).orElse(new ArrayList<>());
    }

    protected boolean isPresent(String key) {
        return resolve(key).isPresent();
    }

    protected boolean isDisplayed(String key) {
        try {
            return find(key).map(WebElement::isDisplayed).orElse(false);
        } catch (StaleElementReferenceException e) {
            return false;
        }
    }

    // ------------------------------------------------------------------ waits

    protected WebDriverWait explicitWait() {
        return new WebDriverWait(driver, explicitTimeout, polling);
    }

    protected WebDriverWait shortWait() {
        return new WebDriverWait(driver, shortTimeout, polling);
    }

    protected FluentWait<WebDriver> fluentWait(Duration timeout) {
        return new FluentWait<>(driver)
                .withTimeout(timeout)
                .pollingEvery(polling)
                .ignoring(NoSuchElementException.class, StaleElementReferenceException.class);
    }

    /** Waits until any fallback locator for the key has a visible element. */
    protected WebElement waitForVisible(String key) {
        return waitForVisible(key, explicitTimeout);
    }

    protected WebElement waitForVisible(String key, Duration timeout) {
        try {
            return fluentWait(timeout).until(d -> {
                for (By by : selectors.locators(key)) {
                    for (WebElement e : d.findElements(by)) {
                        try {
                            if (e.isDisplayed()) {
                                return e;
                            }
                        } catch (StaleElementReferenceException ignored) {
                            // element re-rendered, poll again
                        }
                    }
                }
                return null;
            });
        } catch (TimeoutException e) {
            throw new SelectorNotFoundException(key, "No visible element for '" + key + "' within " + timeout.getSeconds() + "s on " + safeUrl());
        }
    }

    protected WebElement waitForClickable(String key) {
        WebElement el = waitForVisible(key);
        try {
            return explicitWait().until(ExpectedConditions.elementToBeClickable(el));
        } catch (TimeoutException e) {
            throw new SelectorNotFoundException(key, "Element for '" + key + "' not clickable within " + explicitTimeout.getSeconds() + "s");
        }
    }

    protected WebElement waitForPresence(String key) {
        try {
            return fluentWait(explicitTimeout).until(d -> {
                for (By by : selectors.locators(key)) {
                    List<WebElement> l = d.findElements(by);
                    if (!l.isEmpty()) {
                        return l.get(0);
                    }
                }
                return null;
            });
        } catch (TimeoutException e) {
            throw new SelectorNotFoundException(key, "No element for '" + key + "' within " + explicitTimeout.getSeconds() + "s");
        }
    }

    /** True when the key becomes visible within the given timeout; false otherwise (never throws). */
    protected boolean waitForVisibleQuietly(String key, Duration timeout) {
        try {
            waitForVisible(key, timeout);
            return true;
        } catch (SelectorNotFoundException e) {
            return false;
        }
    }

    protected void waitForPageLoad() {
        try {
            explicitWait().until((ExpectedCondition<Boolean>) d ->
                    "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
        } catch (TimeoutException e) {
            log.warn("document.readyState did not reach 'complete' within {}s; continuing", explicitTimeout.getSeconds());
        }
    }

    protected void waitForUrlContains(String fragment) {
        explicitWait().until(ExpectedConditions.urlContains(fragment));
    }

    // ------------------------------------------------------------------ actions

    protected void click(String key) {
        RetryUtil.withRetry("click " + key, config.getRetry().getMaxAttempts(), config.getRetry().getBackoffMillis(), () -> {
            WebElement el = waitForClickable(key);
            scrollIntoView(el);
            try {
                el.click();
            } catch (org.openqa.selenium.ElementClickInterceptedException intercepted) {
                // overlay / sticky header in the way: a JS click is a legitimate fallback for the same element
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
            }
        });
    }

    protected void click(WebElement el) {
        RetryUtil.withRetry("click element", config.getRetry().getMaxAttempts(), config.getRetry().getBackoffMillis(), () -> {
            scrollIntoView(el);
            try {
                el.click();
            } catch (org.openqa.selenium.ElementClickInterceptedException intercepted) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
            }
        });
    }

    protected void type(String key, CharSequence text) {
        RetryUtil.withRetry("type into " + key, config.getRetry().getMaxAttempts(), config.getRetry().getBackoffMillis(), () -> {
            WebElement el = waitForVisible(key);
            typeInto(el, text);
        });
    }

    protected void typeInto(WebElement el, CharSequence text) {
        scrollIntoView(el);
        el.click();
        String contentEditable = el.getAttribute("contenteditable");
        if ("true".equalsIgnoreCase(contentEditable)) {
            el.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
        } else {
            try {
                el.clear();
            } catch (RuntimeException ignored) {
                el.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            }
        }
        el.sendKeys(text);
    }

    protected String getText(String key) {
        return find(key).map(this::safeText).orElse("");
    }

    protected String safeText(WebElement el) {
        try {
            String t = el.getText();
            if (t == null || t.isBlank()) {
                t = el.getAttribute("textContent");
            }
            return t == null ? "" : t.trim();
        } catch (StaleElementReferenceException e) {
            return "";
        }
    }

    protected String safeAttr(WebElement el, String attr) {
        try {
            String v = el.getAttribute(attr);
            return v == null ? "" : v.trim();
        } catch (StaleElementReferenceException e) {
            return "";
        }
    }

    protected void scrollIntoView(WebElement el) {
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center', inline:'nearest'});", el);
        } catch (RuntimeException ignored) {
            // scrolling is best effort
        }
    }

    protected String safeUrl() {
        try {
            return driver.getCurrentUrl();
        } catch (RuntimeException e) {
            return "<unknown url>";
        }
    }

    protected String pageTextLower() {
        try {
            String t = (String) ((JavascriptExecutor) driver).executeScript("return document.body ? document.body.innerText : '';");
            return t == null ? "" : t.toLowerCase();
        } catch (RuntimeException e) {
            return "";
        }
    }

    public WebDriver getDriver() {
        return driver;
    }
}
