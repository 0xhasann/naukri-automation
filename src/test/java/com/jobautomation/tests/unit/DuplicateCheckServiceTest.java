package com.jobautomation.tests.unit;

import com.jobautomation.models.ApplicationRecord;
import com.jobautomation.models.ApplicationStatus;
import com.jobautomation.models.JobDetails;
import com.jobautomation.models.JobListing;
import com.jobautomation.services.DuplicateCheckService;
import com.jobautomation.storage.ApplicationHistoryStore;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class DuplicateCheckServiceTest {

    @Test
    public void appliedJobIsSkipped() {
        ApplicationHistoryStore h = ApplicationHistoryStore.inMemory();
        JobListing l = new JobListing("100", "Java Developer", "Acme", "Bengaluru", "https://www.naukri.com/job-100?src=x");
        h.upsert(ApplicationRecord.of(l, ApplicationStatus.APPLIED, "ok"));
        DuplicateCheckService d = new DuplicateCheckService(h);
        Assert.assertTrue(d.reasonToSkip(l).isPresent());
        Assert.assertTrue(d.reasonToSkip(l).get().contains("Already applied"));
    }

    @Test
    public void rejectedJobIsNotReEvaluated() {
        ApplicationHistoryStore h = ApplicationHistoryStore.inMemory();
        JobListing l = new JobListing("101", "Manual Tester", "Acme", "Pune", "u");
        h.upsert(ApplicationRecord.of(l, ApplicationStatus.REJECTED, "not java"));
        Assert.assertTrue(new DuplicateCheckService(h).reasonToSkip(l).isPresent());
    }

    @Test
    public void failedJobMayBeRetried() {
        ApplicationHistoryStore h = ApplicationHistoryStore.inMemory();
        JobListing l = new JobListing("102", "Java Developer", "Acme", "Pune", "u");
        h.upsert(ApplicationRecord.of(l, ApplicationStatus.FAILED, "timeout"));
        Assert.assertTrue(new DuplicateCheckService(h).reasonToSkip(l).isEmpty());
    }

    @Test
    public void sameRoleAtSameCompanyIsSkipped() {
        ApplicationHistoryStore h = ApplicationHistoryStore.inMemory();
        h.upsert(ApplicationRecord.of(new JobListing("200", "Java Backend Developer", "Acme Pvt Ltd", "Bengaluru", "u1"), ApplicationStatus.APPLIED, "ok"));
        JobDetails other = TestFixtures.job("Java Backend Developer", "acme pvt ltd", "Hyderabad", "4-6", null, List.of("Java"), "x");
        other.setJobId("201");
        Assert.assertTrue(new DuplicateCheckService(h).reasonToSkip(other).isPresent());
        JobDetails different = TestFixtures.job("Senior Java Developer", "Acme Pvt Ltd", "Hyderabad", "4-6", null, List.of("Java"), "x");
        different.setJobId("202");
        Assert.assertTrue(new DuplicateCheckService(h).reasonToSkip(different).isEmpty());
    }

    @Test
    public void appliedRecordIsNeverOverwritten() {
        ApplicationHistoryStore h = ApplicationHistoryStore.inMemory();
        JobListing l = new JobListing("300", "Java Developer", "Acme", "Pune", "u");
        h.upsert(ApplicationRecord.of(l, ApplicationStatus.APPLIED, "ok"));
        h.upsert(ApplicationRecord.of(l, ApplicationStatus.FAILED, "later failure"));
        Assert.assertEquals(h.find("300").get().getStatus(), ApplicationStatus.APPLIED);
        Assert.assertEquals(h.all().size(), 1);
    }

    @Test
    public void canonicalUrlStripsTracking() {
        Assert.assertEquals(DuplicateCheckService.canonicalId("https://www.naukri.com/job-listings-x-123?src=jobsearchDesk&sid=1#apply"),
                "https://www.naukri.com/job-listings-x-123");
    }
}
