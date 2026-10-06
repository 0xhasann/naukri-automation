package com.jobautomation.services;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jobautomation.config.AppConfig;
import com.jobautomation.models.ApplicationRecord;
import com.jobautomation.models.ApplicationStatus;
import com.jobautomation.models.RunSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Daily Application Report: an Extent HTML report plus a JSON and a Markdown copy of the same
 * data under {@code reports/}. Never throws - reporting must not break a run.
 */
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");
    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final AppConfig config;

    public ReportService(AppConfig config) {
        this.config = config;
    }

    public Path generate(RunSummary summary) {
        summary.setFinishedAt(LocalDateTime.now());
        Path dir = Paths.get(config.getPaths().getReports());
        String stamp = summary.getStartedAt().format(STAMP);
        try {
            Files.createDirectories(dir);
            Path html = dir.resolve("daily-application-report_" + stamp + ".html");
            Path json = dir.resolve("daily-application-report_" + stamp + ".json");
            Path md = dir.resolve("daily-application-report_" + stamp + ".md");
            writeExtent(html, summary);
            JSON.writeValue(json.toFile(), toJson(summary));
            Files.writeString(md, toMarkdown(summary));
            log.info("Reports written: {}, {}, {}", html, json, md);
            return html;
        } catch (IOException | RuntimeException e) {
            log.error("Report generation failed: {}", e.getMessage());
            return null;
        }
    }

    private void writeExtent(Path html, RunSummary s) {
        ExtentSparkReporter spark = new ExtentSparkReporter(html.toFile());
        spark.config().setDocumentTitle("Naukri Daily Application Report");
        spark.config().setReportName("Java Developer applications - " + s.getStartedAt().toLocalDate());
        ExtentReports extent = new ExtentReports();
        extent.attachReporter(spark);
        extent.setSystemInfo("Mode", s.getMode());
        extent.setSystemInfo("Daily limit", String.valueOf(s.getDailyLimit()));
        extent.setSystemInfo("Applied before run (today)", String.valueOf(s.getAppliedBeforeRun()));
        extent.setSystemInfo("Cities searched", String.join(", ", s.getCitiesSearched()));
        extent.setSystemInfo("Stop reason", s.getStopReason());

        ExtentTest totals = extent.createTest("Totals");
        totals.info("Discovered: " + s.getDiscovered());
        totals.info("Already processed in earlier runs: " + s.getAlreadyProcessed());
        totals.info("Evaluated: " + s.getEvaluated());
        totals.info("Rejected: " + s.getRejected());
        totals.info("Skipped: " + s.getSkipped());
        totals.info("Applied: " + s.getApplied());
        totals.info("Would apply (non-LIVE): " + s.getWouldApply());
        totals.info("Failed: " + s.getFailed());
        totals.info("Manual review required: " + s.getManualReview());
        totals.info("Applications remaining today: " + s.applicationsRemaining() + " / " + s.getDailyLimit());

        List<ApplicationRecord> rows = s.getRows();
        rows.stream().sorted(Comparator.comparing((ApplicationRecord r) -> r.getStatus().ordinal())
                        .thenComparing(r -> r.getMatchScore() == null ? 0 : -r.getMatchScore()))
                .forEach(r -> {
                    ExtentTest t = extent.createTest(r.getStatus() + " | " + r.getTitle() + " @ " + r.getCompany());
                    t.info("City: " + (r.getSearchCity() != null ? r.getSearchCity() : r.getLocation()));
                    t.info("Location: " + r.getLocation());
                    t.info("Match score: " + (r.getMatchScore() == null ? "-" : r.getMatchScore() + "%"));
                    t.info("Salary: " + (r.getSalary() == null || r.getSalary().isBlank() ? "not disclosed" : r.getSalary()));
                    t.info("Experience: " + r.getExperience());
                    t.info("URL: " + r.getUrl());
                    t.info("Reason: " + r.getReason());
                    if (r.getScreenshot() != null) {
                        try {
                            t.addScreenCaptureFromPath(r.getScreenshot());
                        } catch (RuntimeException ignored) {
                            t.info("Screenshot: " + r.getScreenshot());
                        }
                    }
                    t.log(statusOf(r.getStatus()), r.getStatus().name());
                });
        extent.flush();
    }

    private static Status statusOf(ApplicationStatus s) {
        return switch (s) {
            case APPLIED, WOULD_APPLY -> Status.PASS;
            case FAILED -> Status.FAIL;
            case REQUIRES_MANUAL_REVIEW -> Status.WARNING;
            default -> Status.SKIP;
        };
    }

    private static Map<String, Object> toJson(RunSummary s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("startedAt", s.getStartedAt());
        m.put("finishedAt", s.getFinishedAt());
        m.put("mode", s.getMode());
        m.put("dailyLimit", s.getDailyLimit());
        m.put("appliedBeforeRun", s.getAppliedBeforeRun());
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("discovered", s.getDiscovered());
        totals.put("alreadyProcessed", s.getAlreadyProcessed());
        totals.put("evaluated", s.getEvaluated());
        totals.put("rejected", s.getRejected());
        totals.put("skipped", s.getSkipped());
        totals.put("applied", s.getApplied());
        totals.put("wouldApply", s.getWouldApply());
        totals.put("failed", s.getFailed());
        totals.put("manualReview", s.getManualReview());
        totals.put("applicationsRemaining", s.applicationsRemaining());
        m.put("totals", totals);
        m.put("citiesSearched", s.getCitiesSearched());
        m.put("stopReason", s.getStopReason());
        m.put("jobs", s.getRows());
        return m;
    }

    static String toMarkdown(RunSummary s) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Daily Application Report - ").append(s.getStartedAt().toLocalDate()).append("\n\n");
        sb.append("Mode: **").append(s.getMode()).append("**  \n");
        sb.append("Stop reason: ").append(s.getStopReason()).append("  \n");
        sb.append("Cities searched: ").append(String.join(", ", s.getCitiesSearched())).append("\n\n");
        sb.append("| Metric | Value |\n|---|---|\n");
        sb.append("| Jobs discovered | ").append(s.getDiscovered()).append(" |\n");
        sb.append("| Already processed in earlier runs | ").append(s.getAlreadyProcessed()).append(" |\n");
        sb.append("| Jobs evaluated | ").append(s.getEvaluated()).append(" |\n");
        sb.append("| Rejected | ").append(s.getRejected()).append(" |\n");
        sb.append("| Skipped | ").append(s.getSkipped()).append(" |\n");
        sb.append("| Applied | ").append(s.getApplied()).append(" |\n");
        sb.append("| Would apply (non-LIVE) | ").append(s.getWouldApply()).append(" |\n");
        sb.append("| Failed | ").append(s.getFailed()).append(" |\n");
        sb.append("| Manual review required | ").append(s.getManualReview()).append(" |\n");
        sb.append("| Applications remaining today | ").append(s.applicationsRemaining()).append(" / ").append(s.getDailyLimit()).append(" |\n\n");
        sb.append("| Status | City | Company | Title | Score | Salary | Reason |\n|---|---|---|---|---|---|---|\n");
        for (ApplicationRecord r : s.getRows()) {
            sb.append("| ").append(r.getStatus())
                    .append(" | ").append(nz(r.getSearchCity() != null ? r.getSearchCity() : r.getLocation()))
                    .append(" | ").append(nz(r.getCompany()))
                    .append(" | [").append(nz(r.getTitle())).append("](").append(nz(r.getUrl())).append(")")
                    .append(" | ").append(r.getMatchScore() == null ? "-" : r.getMatchScore() + "%")
                    .append(" | ").append(r.getSalary() == null || r.getSalary().isBlank() ? "n/a" : r.getSalary())
                    .append(" | ").append(nz(r.getReason()).replace("|", "/").replace("\n", " "))
                    .append(" |\n");
        }
        return sb.toString();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
