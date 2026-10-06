package com.jobautomation;

import com.jobautomation.config.AppConfig;
import com.jobautomation.config.ConfigLoader;
import com.jobautomation.models.RunSummary;
import com.jobautomation.services.JobAutomationRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plain entry point (no TestNG):
 * <pre>
 *   mvn exec:java -Dmode=DRY_RUN
 *   mvn exec:java -Dmode=TEST
 *   mvn exec:java -Dmode=LIVE -DdailyApplicationLimit=20
 * </pre>
 * Exit code 0 when the run completed or stopped for an expected reason (daily limit),
 * 1 when it stopped because of login failure / security challenge / configuration.
 */
public final class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length > 0 && System.getProperty("mode") == null) {
            System.setProperty("mode", args[0]);
        }
        AppConfig config = ConfigLoader.get();
        log.info("Mode: {}", config.runMode());
        RunSummary summary = new JobAutomationRunner(config).run();
        String reason = summary.getStopReason() == null ? "" : summary.getStopReason();
        boolean ok = reason.startsWith("All cities") || reason.startsWith("DAILY_LIMIT_REACHED") || reason.startsWith("Stopped by request");
        System.exit(ok ? 0 : 1);
    }
}
