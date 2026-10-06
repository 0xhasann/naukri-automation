package com.jobautomation.filters;

import com.jobautomation.config.AppConfig;
import com.jobautomation.matcher.TextNormalizer;
import com.jobautomation.models.JobDetails;

import java.util.Locale;

/**
 * Configurable company screening: blocked list, preferred list (small bonus), and
 * staffing/spam indicators. With no lists configured nothing legitimate is blocked.
 */
public class CompanyFilter {

    public static final class Result {
        public final boolean blocked;
        public final boolean preferred;
        public final boolean spam;
        public final String reason;

        Result(boolean blocked, boolean preferred, boolean spam, String reason) {
            this.blocked = blocked;
            this.preferred = preferred;
            this.spam = spam;
            this.reason = reason;
        }

        public boolean reject() {
            return blocked || spam;
        }
    }

    private final AppConfig config;

    public CompanyFilter(AppConfig config) {
        this.config = config;
    }

    public Result evaluate(JobDetails job) {
        String company = job.getCompany() == null ? "" : job.getCompany().trim();
        String companyLower = company.toLowerCase(Locale.ROOT);
        if (company.isEmpty()) {
            return new Result(false, false, false, "Company name not extracted");
        }
        for (String b : config.getCompanies().getBlocked()) {
            if (!b.isBlank() && companyLower.contains(b.trim().toLowerCase(Locale.ROOT))) {
                return new Result(true, false, false, "Company is on the blocked list: " + b);
            }
        }
        String jdNorm = TextNormalizer.normalize(job.getDescription());
        for (String s : config.getCompanies().getSpamIndicators()) {
            String ind = TextNormalizer.normalize(s);
            if (ind.isEmpty()) {
                continue;
            }
            if (companyLower.contains(ind)) {
                return new Result(false, false, true, "Company name looks like a staffing/consultancy listing (" + s + ")");
            }
            // In the JD, only the strongest indicators count (fees, walk-ins, "hiring for our client")
            if ((ind.contains("fee") || ind.contains("walk") || ind.contains("our client") || ind.contains("pay and work"))
                    && jdNorm.contains(ind)) {
                return new Result(false, false, true, "JD contains spam indicator '" + s + "'");
            }
        }
        for (String p : config.getCompanies().getPreferred()) {
            if (!p.isBlank() && companyLower.contains(p.trim().toLowerCase(Locale.ROOT))) {
                return new Result(false, true, false, "Preferred company: " + p);
            }
        }
        return new Result(false, false, false, "Company acceptable");
    }
}
