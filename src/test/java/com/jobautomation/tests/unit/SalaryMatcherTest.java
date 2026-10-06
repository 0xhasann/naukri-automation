package com.jobautomation.tests.unit;

import com.jobautomation.config.AppConfig;
import com.jobautomation.matcher.JdParser;
import com.jobautomation.matcher.SalaryMatcher;
import com.jobautomation.models.JobDetails;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SalaryMatcherTest {

    private static JobDetails withSalary(String text) {
        JobDetails d = new JobDetails();
        d.setSalaryText(text);
        d.setDescription("");
        JdParser.enrich(d);
        return d;
    }

    @Test
    public void parsesNaukriSalaryFormats() {
        Assert.assertEquals(JdParser.parseSalaryLpa("20-30 Lacs PA"), new double[]{20, 30});
        Assert.assertEquals(JdParser.parseSalaryLpa("20 - 30 Lacs P.A."), new double[]{20, 30});
        Assert.assertEquals(JdParser.parseSalaryLpa("₹ 15,00,000 - 25,00,000 P.A."), new double[]{15, 25});
        Assert.assertEquals(JdParser.parseSalaryLpa("25 LPA"), new double[]{25, 25});
        Assert.assertEquals(JdParser.parseSalaryLpa("1 - 1.5 Cr"), new double[]{100, 150});
        Assert.assertNull(JdParser.parseSalaryLpa("Not Disclosed"));
        Assert.assertNull(JdParser.parseSalaryLpa(null));
    }

    @Test
    public void belowMinimumRejects() {
        SalaryMatcher m = new SalaryMatcher(TestFixtures.config());
        Assert.assertTrue(m.evaluate(withSalary("10-18 Lacs PA")).reject);
    }

    @Test
    public void rangeReachingMinimumIsAccepted() {
        SalaryMatcher m = new SalaryMatcher(TestFixtures.config());
        Assert.assertFalse(m.evaluate(withSalary("15-25 Lacs PA")).reject);
        Assert.assertFalse(m.evaluate(withSalary("20-30 Lacs PA")).reject);
    }

    @Test
    public void undisclosedIsAcceptedByDefaultAndRejectableByConfig() {
        AppConfig c = TestFixtures.config();
        Assert.assertFalse(new SalaryMatcher(c).evaluate(withSalary("Not Disclosed")).reject);
        c.setRejectUndisclosedSalary(true);
        Assert.assertTrue(new SalaryMatcher(c).evaluate(withSalary("Not Disclosed")).reject);
    }
}
