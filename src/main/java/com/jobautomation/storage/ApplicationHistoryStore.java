package com.jobautomation.storage;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jobautomation.models.ApplicationRecord;
import com.jobautomation.models.ApplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Persistent application history (JSON file). Survives restarts and crashes:
 * every mutation is written atomically (write temp file, then move) so a crash mid-write
 * never corrupts the history.
 */
public class ApplicationHistoryStore {

    private static final Logger log = LoggerFactory.getLogger(ApplicationHistoryStore.class);
    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final Path file;
    private HistoryState state;

    public ApplicationHistoryStore(Path file) {
        this.file = file;
        this.state = load();
    }

    public static ApplicationHistoryStore inMemory() {
        return new ApplicationHistoryStore(null);
    }

    private HistoryState load() {
        if (file == null || !Files.exists(file)) {
            return new HistoryState();
        }
        try {
            HistoryState s = JSON.readValue(Files.readAllBytes(file), HistoryState.class);
            log.info("Loaded application history: {} records, last processed job {}",
                    s.getRecords().size(), s.getLastProcessedJobId());
            return s == null ? new HistoryState() : s;
        } catch (IOException e) {
            // Never lose the file: back it up and start clean rather than crash the run.
            Path backup = file.resolveSibling(file.getFileName() + ".corrupt-" + System.currentTimeMillis());
            try {
                Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // best effort
            }
            log.error("application history unreadable ({}). Backed up to {} and starting a fresh history.", e.getMessage(), backup);
            return new HistoryState();
        }
    }

    public synchronized void save() {
        if (file == null) {
            return;
        }
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            JSON.writeValue(tmp.toFile(), state);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicUnsupported) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("Could not persist application history to {}: {}", file, e.getMessage());
        }
    }

    // ------------------------------------------------------------------ writes

    /** Adds or replaces the record for the job (one record per job id, latest status wins). */
    public synchronized void upsert(ApplicationRecord record) {
        if (record.getJobId() != null) {
            state.getRecords().removeIf(r -> record.getJobId().equals(r.getJobId())
                    && r.getStatus() != ApplicationStatus.APPLIED);   // an APPLIED record is never overwritten
            boolean alreadyApplied = state.getRecords().stream()
                    .anyMatch(r -> record.getJobId().equals(r.getJobId()) && r.getStatus() == ApplicationStatus.APPLIED);
            if (alreadyApplied && record.getStatus() != ApplicationStatus.APPLIED) {
                log.debug("Ignoring {} for {} because an APPLIED record already exists", record.getStatus(), record.getJobId());
                return;
            }
            if (alreadyApplied) {
                return;
            }
            state.setLastProcessedJobId(record.getJobId());
        }
        state.getRecords().add(record);
        save();
    }

    public synchronized void markSearchProgress(String city, String keyword, int page) {
        state.setLastSearchCity(city);
        state.setLastSearchKeyword(keyword);
        state.setLastSearchPage(page);
        save();
    }

    // ------------------------------------------------------------------ reads

    public synchronized List<ApplicationRecord> all() {
        return Collections.unmodifiableList(new ArrayList<>(state.getRecords()));
    }

    public synchronized Optional<ApplicationRecord> find(String jobId) {
        if (jobId == null) {
            return Optional.empty();
        }
        return state.getRecords().stream().filter(r -> jobId.equals(r.getJobId())).findFirst();
    }

    public synchronized boolean isProcessed(String jobId) {
        return find(jobId).isPresent();
    }

    public synchronized boolean isApplied(String jobId) {
        return find(jobId).map(r -> r.getStatus() == ApplicationStatus.APPLIED).orElse(false);
    }

    /** True when an APPLIED record exists for the same company + normalised title (duplicate role at same company). */
    public synchronized boolean isAppliedToSameRole(String company, String title) {
        if (company == null || title == null) {
            return false;
        }
        String c = company.trim().toLowerCase(Locale.ROOT);
        String t = normaliseTitle(title);
        return state.getRecords().stream().anyMatch(r ->
                r.getStatus() == ApplicationStatus.APPLIED
                        && r.getCompany() != null && r.getTitle() != null
                        && r.getCompany().trim().toLowerCase(Locale.ROOT).equals(c)
                        && normaliseTitle(r.getTitle()).equals(t));
    }

    public synchronized int appliedOn(LocalDate day) {
        return (int) state.getRecords().stream()
                .filter(r -> r.getStatus() == ApplicationStatus.APPLIED)
                .filter(r -> r.getTimestamp() != null && r.getTimestamp().toLocalDate().equals(day))
                .count();
    }

    public synchronized int appliedToday() {
        return appliedOn(LocalDate.now());
    }

    public synchronized String lastProcessedJobId() {
        return state.getLastProcessedJobId();
    }

    public synchronized HistoryState snapshot() {
        return state;
    }

    static String normaliseTitle(String title) {
        return title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    public Path getFile() {
        return file;
    }
}
