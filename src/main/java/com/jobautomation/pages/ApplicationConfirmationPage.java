package com.jobautomation.pages;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

import java.time.Duration;

/** Reads the "You have successfully applied" confirmation (or an explicit failure banner). */
public class ApplicationConfirmationPage extends BasePage {

    public ApplicationConfirmationPage(WebDriver driver) {
        super(driver);
    }

    public boolean isSuccessVisible() {
        return isDisplayed("confirmation.success");
    }

    public boolean isFailureVisible() {
        return isDisplayed("confirmation.failure");
    }

    public boolean waitForSuccess(Duration timeout) {
        try {
            return fluentWait(timeout).until(d -> isSuccessVisible() || isFailureVisible()) && isSuccessVisible();
        } catch (TimeoutException e) {
            return false;
        }
    }

    public String message() {
        return getText("confirmation.success");
    }
}
