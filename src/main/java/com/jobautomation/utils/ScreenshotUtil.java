package com.jobautomation.utils;

import com.jobautomation.config.ConfigLoader;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Saves screenshots under {@code screenshots/YYYY-MM-DD/<label>_<time>.png}. Never throws. */
public final class ScreenshotUtil {

    private static final Logger log = LoggerFactory.getLogger(ScreenshotUtil.class);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HHmmss_SSS");

    private ScreenshotUtil() {
    }

    /** @return the saved path, or null when the screenshot could not be taken. */
    public static String capture(WebDriver driver, String label) {
        if (driver == null) {
            return null;
        }
        try {
            Path dir = Paths.get(ConfigLoader.get().getPaths().getScreenshots(), LocalDate.now().toString());
            Files.createDirectories(dir);
            String safe = label == null ? "screenshot" : label.replaceAll("[^A-Za-z0-9._-]+", "_");
            if (safe.length() > 80) {
                safe = safe.substring(0, 80);
            }
            Path target = dir.resolve(safe + "_" + LocalDateTime.now().format(TIME) + ".png");
            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Files.write(target, png);
            log.info("Screenshot saved: {}", target);
            return target.toString();
        } catch (IOException | RuntimeException e) {
            log.warn("Could not capture screenshot '{}': {}", label, e.getMessage());
            return null;
        }
    }
}
