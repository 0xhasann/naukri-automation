package com.jobautomation.tests.unit;

import com.jobautomation.exceptions.DailyLimitReachedException;
import com.jobautomation.models.ApplicationRecord;
import com.jobautomation.models.ApplicationStatus;
import com.jobautomation.models.JobListing;
import com.jobautomation.services.DailyApplicationLimitService;
import com.jobautomation.storage.ApplicationHistoryStore;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class DailyLimitServiceTest {

    private static ApplicationRecord applied(String id, LocalDateTime when) {
        ApplicationRecord r = ApplicationRecord.of(new JobListing(id, "Java Dev " + id, "Co " + id, "Bengaluru", "https://x/" + id), ApplicationStatus.APPLIED, "ok");
        r.setTimestamp(when);
        return r;
    }

    @Test
    public void countsOnlyTodaysApplications() {
        ApplicationHistoryStore h = ApplicationHistoryStore.inMemory();
        h.upsert(applied("1", LocalDateTime.now()));
        h.upsert(applied("2", LocalDateTime.now().minusDays(1)));
        h.upsert(ApplicationRecord.of(new JobListing("3", "t", "c", "l", "u"), ApplicationStatus.REJECTED, "no"));
        DailyApplicationLimitService s = new DailyApplicationLimitService(h, 5);
        Assert.assertEquals(s.applicationsToday(), 1);
        Assert.assertEquals(s.remaining(), 4);
        Assert.assertFalse(s.isReached());
    }

    @Test(expectedExceptions = DailyLimitReachedException.class)
    public void stopsExactlyAtLimit() {
        ApplicationHistoryStore h = ApplicationHistoryStore.inMemory();
        DailyApplicationLimitService s = new DailyApplicationLimitService(h, 2);
        h.upsert(applied("1", LocalDateTime.now()));
        s.assertNotReached();
        h.upsert(applied("2", LocalDateTime.now()));
        Assert.assertTrue(s.isReached());
        s.assertNotReached();   // throws
    }

    @Test
    public void limitSurvivesRestartViaPersistedHistory() throws Exception {
        Path tmp = Files.createTempFile("history", ".json");
        Files.delete(tmp);
        ApplicationHistoryStore first = new ApplicationHistoryStore(tmp);
        first.upsert(applied("10", LocalDateTime.now()));
        first.upsert(applied("11", LocalDateTime.now()));
        ApplicationHistoryStore reloaded = new ApplicationHistoryStore(tmp);
        Assert.assertEquals(new DailyApplicationLimitService(reloaded, 2).applicationsToday(), 2);
        Assert.assertTrue(new DailyApplicationLimitService(reloaded, 2).isReached());
        Assert.assertEquals(reloaded.lastProcessedJobId(), "11");
        Files.deleteIfExists(tmp);
    }

    @Test
    public void zeroLimitMeansNoApplications() {
        DailyApplicationLimitService s = new DailyApplicationLimitService(ApplicationHistoryStore.inMemory(), 0);
        Assert.assertTrue(s.isReached());
    }
}
