package com.jobautomation.matcher;

import com.jobautomation.models.JobDetails;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts structured facts (experience range, salary range, work mode) from the free text
 * shown on Naukri listings and job pages.
 */
public final class JdParser {

    /** e.g. "4 - 7 years", "4-7 Yrs", "5+ years", "3 to 6 yrs", "Minimum 4 years", "at least 5 years". */
    private static final Pattern EXP_RANGE = Pattern.compile("(\\d{1,2})\\s*(?:-|–|to)\\s*(\\d{1,2})\\s*\\+?\\s*(?:years?|yrs?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXP_PLUS = Pattern.compile("(\\d{1,2})\\s*\\+\\s*(?:years?|yrs?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXP_MIN = Pattern.compile("(?:minimum|min\\.?|at least|atleast|over|more than)\\s*(?:of\\s*)?(\\d{1,2})\\s*\\+?\\s*(?:years?|yrs?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXP_SINGLE = Pattern.compile("(\\d{1,2})\\s*(?:years?|yrs?)\\s*(?:of\\s*)?(?:experience|exp)", Pattern.CASE_INSENSITIVE);

    /** e.g. "20-30 Lacs PA", "20 - 30 Lacs P.A.", "₹ 15,00,000 - 25,00,000 P.A.", "25 LPA", "1 - 1.5 Cr". */
    private static final Pattern SAL_LACS_RANGE = Pattern.compile("(\\d{1,3}(?:\\.\\d{1,2})?)\\s*(?:-|–|to)\\s*(\\d{1,3}(?:\\.\\d{1,2})?)\\s*(?:lacs?|lakhs?|lpa|l\\b)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SAL_LACS_SINGLE = Pattern.compile("(\\d{1,3}(?:\\.\\d{1,2})?)\\s*(?:lacs?|lakhs?|lpa|l\\b)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SAL_CR_RANGE = Pattern.compile("(\\d{1,2}(?:\\.\\d{1,2})?)\\s*(?:-|–|to)\\s*(\\d{1,2}(?:\\.\\d{1,2})?)\\s*(?:cr|crores?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SAL_RUPEES_RANGE = Pattern.compile("(\\d{1,2}(?:,\\d{2})*,\\d{3})\\s*(?:-|–|to)\\s*(\\d{1,2}(?:,\\d{2})*,\\d{3})", Pattern.CASE_INSENSITIVE);
    private static final Pattern NOT_DISCLOSED = Pattern.compile("not\\s*disclosed|undisclosed|confidential|as per (?:industry|company)", Pattern.CASE_INSENSITIVE);

    private JdParser() {
    }

    // ------------------------------------------------------------------ experience

    public static int[] parseExperienceRange(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher m = EXP_RANGE.matcher(text);
        if (m.find()) {
            int lo = Integer.parseInt(m.group(1));
            int hi = Integer.parseInt(m.group(2));
            return new int[]{Math.min(lo, hi), Math.max(lo, hi)};
        }
        m = EXP_PLUS.matcher(text);
        if (m.find()) {
            return new int[]{Integer.parseInt(m.group(1)), -1};
        }
        m = EXP_MIN.matcher(text);
        if (m.find()) {
            return new int[]{Integer.parseInt(m.group(1)), -1};
        }
        m = EXP_SINGLE.matcher(text);
        if (m.find()) {
            return new int[]{Integer.parseInt(m.group(1)), -1};
        }
        return null;
    }

    // ------------------------------------------------------------------ salary

    /** @return {min, max} in LPA, or null when undisclosed / not parseable. */
    public static double[] parseSalaryLpa(String text) {
        if (text == null || text.isBlank() || NOT_DISCLOSED.matcher(text).find()) {
            return null;
        }
        Matcher m = SAL_CR_RANGE.matcher(text);
        if (m.find()) {
            return new double[]{Double.parseDouble(m.group(1)) * 100, Double.parseDouble(m.group(2)) * 100};
        }
        m = SAL_LACS_RANGE.matcher(text);
        if (m.find()) {
            double lo = Double.parseDouble(m.group(1));
            double hi = Double.parseDouble(m.group(2));
            return new double[]{Math.min(lo, hi), Math.max(lo, hi)};
        }
        m = SAL_RUPEES_RANGE.matcher(text);
        if (m.find()) {
            double lo = Double.parseDouble(m.group(1).replace(",", "")) / 100000.0;
            double hi = Double.parseDouble(m.group(2).replace(",", "")) / 100000.0;
            return new double[]{Math.min(lo, hi), Math.max(lo, hi)};
        }
        m = SAL_LACS_SINGLE.matcher(text);
        if (m.find()) {
            double v = Double.parseDouble(m.group(1));
            return new double[]{v, v};
        }
        return null;
    }

    // ------------------------------------------------------------------ work mode

    public static Optional<String> detectWorkMode(String text) {
        if (text == null) {
            return Optional.empty();
        }
        String t = text.toLowerCase(Locale.ROOT);
        if (t.contains("remote") || t.contains("work from home") || t.contains("wfh")) {
            return Optional.of("Remote");
        }
        if (t.contains("hybrid")) {
            return Optional.of("Hybrid");
        }
        if (t.contains("work from office") || t.contains("on-site") || t.contains("onsite") || t.contains("in office")) {
            return Optional.of("Onsite");
        }
        return Optional.empty();
    }

    /** Fills the numeric fields of a JobDetails from its text fields. */
    public static void enrich(JobDetails d) {
        int[] exp = parseExperienceRange(d.getExperienceText());
        if (exp == null) {
            exp = parseExperienceRange(d.getDescription());
        }
        if (exp != null) {
            d.setMinExperienceYears(exp[0]);
            d.setMaxExperienceYears(exp[1] < 0 ? null : exp[1]);
        }
        double[] sal = parseSalaryLpa(d.getSalaryText());
        if (sal != null) {
            d.setSalaryDisclosed(true);
            d.setMinSalaryLpa(sal[0]);
            d.setMaxSalaryLpa(sal[1]);
        } else {
            d.setSalaryDisclosed(false);
        }
        if (d.getWorkMode() == null || d.getWorkMode().isBlank()) {
            detectWorkMode(d.getLocation() + " " + d.getDescription()).ifPresent(d::setWorkMode);
        }
    }
}
