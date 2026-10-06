package com.jobautomation.questions;

import com.jobautomation.matcher.TextNormalizer;
import com.jobautomation.models.Answer;
import com.jobautomation.models.CandidateProfile;
import com.jobautomation.models.Question;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Turns a classified question into an answer using ONLY the verified profile.
 * <ul>
 *   <li>Profile value present  -> ANSWERED</li>
 *   <li>Profile value null     -> MISSING_DATA (application is stopped, never guessed)</li>
 *   <li>Category UNKNOWN       -> UNKNOWN (application is stopped, never guessed)</li>
 * </ul>
 * A technology that is not on the profile is treated as unknown, not as "No": add it to
 * {@code skillExperienceYears} with 0 to state explicitly that there is no experience.
 */
public class AnswerResolver {

    private final CandidateProfile profile;

    public AnswerResolver(CandidateProfile profile) {
        this.profile = profile;
    }

    public Answer resolve(Question question, QuestionClassifier.Classification c) {
        String q = TextNormalizer.normalize(question.getText());
        switch (c.category) {
            case RELOCATION:
                return bool(c.category, profile.getRelocationWilling(), "relocationWilling");
            case IMMEDIATE_JOINER:
                return bool(c.category, profile.getImmediateJoiner(), "immediateJoiner");
            case NOTICE_PERIOD:
                if (profile.getNoticePeriodDays() != null) {
                    int days = profile.getNoticePeriodDays();
                    if (!question.getOptions().isEmpty()) {
                        return chooseNoticeOption(question.getOptions(), days);
                    }
                    return Answer.of(c.category, days == 0 ? "Immediate" : days + " days", "noticePeriodDays=" + days);
                }
                if (Boolean.TRUE.equals(profile.getImmediateJoiner()) && !question.getOptions().isEmpty()) {
                    return chooseNoticeOption(question.getOptions(), 0);
                }
                if (Boolean.TRUE.equals(profile.getImmediateJoiner())) {
                    return Answer.of(c.category, "Immediate", "immediateJoiner=true");
                }
                return Answer.missing(c.category, "noticePeriodDays not configured");
            case TOTAL_EXPERIENCE:
                return years(c.category, profile.getTotalExperienceYears(), "totalExperienceYears", question);
            case RELEVANT_JAVA_EXPERIENCE:
                return years(c.category, profile.getRelevantJavaExperienceYears() != null
                        ? profile.getRelevantJavaExperienceYears() : profile.getTotalExperienceYears(), "relevantJavaExperienceYears", question);
            case TECHNOLOGY_EXPERIENCE_YEARS: {
                Optional<Double> y = profile.yearsFor(c.technology);
                if (y.isPresent()) {
                    return years(c.category, y.get(), "skillExperienceYears[" + c.technology + "]", question);
                }
                if (profile.hasSkill(c.technology)) {
                    return Answer.missing(c.category, "Skill '" + c.technology + "' is on the profile but has no configured years (skillExperienceYears)");
                }
                return Answer.missing(c.category, "Technology '" + c.technology + "' is not on the verified profile (add it to skillExperienceYears, 0 if none)");
            }
            case TECHNOLOGY_YES_NO: {
                Optional<Double> y = profile.yearsFor(c.technology);
                if (y.isPresent()) {
                    return bool(c.category, y.get() > 0, "skillExperienceYears[" + c.technology + "]=" + y.get());
                }
                if (profile.hasSkill(c.technology)) {
                    return bool(c.category, true, "skill on profile: " + c.technology);
                }
                return Answer.missing(c.category, "Technology '" + c.technology + "' is not on the verified profile - not answering YES or NO");
            }
            case CURRENT_LOCATION:
                return text(c.category, profile.getCurrentLocation(), "currentLocation", question);
            case PREFERRED_LOCATION:
                return preferredLocation(question, q);
            case WORK_AUTHORIZATION:
                if (profile.getWorkAuthorizationCountries().isEmpty()) {
                    return Answer.missing(c.category, "workAuthorizationCountries not configured");
                }
                if (q.contains("india")) {
                    boolean india = profile.getWorkAuthorizationCountries().stream().anyMatch(s -> s.equalsIgnoreCase("India"));
                    return bool(c.category, india, "workAuthorizationCountries");
                }
                return Answer.missing(c.category, "Work authorization question for an unspecified country");
            case VISA_SPONSORSHIP:
                if (profile.getRequiresVisaSponsorship() == null) {
                    return Answer.missing(c.category, "requiresVisaSponsorship not configured");
                }
                if (q.contains("sponsor") || q.contains("require") || q.contains("need")) {
                    return bool(c.category, profile.getRequiresVisaSponsorship(), "requiresVisaSponsorship");
                }
                return Answer.unknown("Visa question that is not about sponsorship: " + question.getText());
            case EXPECTED_SALARY:
                return salary(c.category, profile.getExpectedSalaryLpa(), "expectedSalaryLpa", question);
            case CURRENT_SALARY:
                return salary(c.category, profile.getCurrentSalaryLpa(), "currentSalaryLpa", question);
            case CURRENT_COMPANY: {
                if (!Boolean.TRUE.equals(profile.getCurrentlyEmployed())) {
                    return Answer.of(c.category, "Not currently employed", "currentlyEmployed=false");
                }
                List<CandidateProfile.Employment> h = profile.getEmploymentHistory();
                return h.isEmpty() ? Answer.missing(c.category, "employmentHistory empty") : text(c.category, h.get(0).getCompany(), "employmentHistory[0].company", question);
            }
            case CURRENT_DESIGNATION:
                return text(c.category, profile.getCurrentTitle(), "currentTitle", question);
            case HIGHEST_QUALIFICATION:
                return text(c.category, profile.getHighestQualification(), "highestQualification", question);
            case DATE_OF_BIRTH:
                return text(c.category, profile.getDob(), "dob", question);
            case AGE:
                if (profile.getAge() != null) {
                    return Answer.of(c.category, String.valueOf(profile.getAge()), "age");
                }
                if (profile.getDob() != null) {
                    try {
                        int age = Period.between(LocalDate.parse(profile.getDob(), DateTimeFormatter.ISO_DATE), LocalDate.now()).getYears();
                        return Answer.of(c.category, String.valueOf(age), "derived from dob");
                    } catch (Exception e) {
                        return Answer.missing(c.category, "dob is not in YYYY-MM-DD format");
                    }
                }
                return Answer.missing(c.category, "age/dob not configured");
            case GENDER:
                return text(c.category, profile.getGender(), "gender", question);
            case NAME:
                return text(c.category, profile.getName(), "name", question);
            case EMAIL:
                return text(c.category, profile.getEmail(), "email", question);
            case PHONE:
                return text(c.category, profile.getPhone(), "phone", question);
            case LINKEDIN:
                return text(c.category, profile.getLinkedin(), "linkedin", question);
            case WORK_MODE_PREFERENCE:
                // Being asked whether office/hybrid is acceptable: acceptable when relocation/preferred locations allow it
                if (Boolean.TRUE.equals(profile.getRelocationWilling())) {
                    return bool(c.category, true, "relocationWilling=true implies office/hybrid acceptable");
                }
                return Answer.missing(c.category, "work-mode preference cannot be derived from profile");
            case CONFIRMATION:
                return bool(c.category, true, "confirmation of provided (verified) details");
            case INFORMATIONAL:
                return Answer.skip(c.category, "Chatbot greeting / instructions, not a screening question");
            case SHIFT_PREFERENCE:
            case UNKNOWN:
            default:
                return Answer.unknown("No verified profile data for question: " + question.getText());
        }
    }

    // ------------------------------------------------------------------ helpers

    private static Answer bool(QuestionCategory cat, Boolean value, String source) {
        if (value == null) {
            return Answer.missing(cat, source + " not configured");
        }
        return Answer.of(cat, value ? "Yes" : "No", source + "=" + value);
    }

    private static Answer text(QuestionCategory cat, String value, String source, Question question) {
        if (value == null || value.isBlank()) {
            return Answer.missing(cat, source + " not configured");
        }
        if (!question.getOptions().isEmpty()) {
            Optional<String> opt = closestOption(question.getOptions(), value);
            return opt.map(o -> Answer.of(cat, o, source + " matched option")).orElseGet(() ->
                    Answer.missing(cat, "None of the options " + question.getOptions() + " matches " + source + "=" + value));
        }
        return Answer.of(cat, value, source);
    }

    private static Answer years(QuestionCategory cat, Double years, String source, Question question) {
        if (years == null) {
            return Answer.missing(cat, source + " not configured");
        }
        if (!question.getOptions().isEmpty()) {
            return chooseYearsOption(cat, question.getOptions(), years, source);
        }
        String v = years == Math.floor(years) ? String.valueOf(years.intValue()) : String.format(Locale.ROOT, "%.1f", years);
        return Answer.of(cat, v, source + "=" + years);
    }

    private static Answer salary(QuestionCategory cat, Double lpa, String source, Question question) {
        if (lpa == null) {
            return Answer.missing(cat, source + " not configured");
        }
        if (!question.getOptions().isEmpty()) {
            return chooseRangeOption(cat, question.getOptions(), lpa, source);
        }
        String q = TextNormalizer.normalize(question.getText());
        // Forms sometimes want the absolute rupee figure rather than LPA
        if (q.contains("in rupees") || q.contains("in inr") || q.contains("annual salary in") || q.contains("per annum in")) {
            return Answer.of(cat, String.valueOf(Math.round(lpa * 100000)), source + "=" + lpa + " LPA (rupees)");
        }
        String v = lpa == Math.floor(lpa) ? String.valueOf(lpa.intValue()) : String.format(Locale.ROOT, "%.1f", lpa);
        return Answer.of(cat, v, source + "=" + lpa + " LPA");
    }

    private Answer preferredLocation(Question question, String q) {
        // "Are you comfortable working in Hyderabad?" -> yes if preferred, or relocation acceptable
        for (String loc : profile.getPreferredLocations()) {
            if (q.contains(loc.toLowerCase(Locale.ROOT))) {
                return Answer.of(QuestionCategory.PREFERRED_LOCATION, "Yes", "preferredLocations contains " + loc);
            }
        }
        if (!question.getOptions().isEmpty()) {
            for (String loc : profile.getPreferredLocations()) {
                Optional<String> opt = closestOption(question.getOptions(), loc);
                if (opt.isPresent()) {
                    return Answer.of(QuestionCategory.PREFERRED_LOCATION, opt.get(), "preferredLocations option");
                }
            }
            return Answer.missing(QuestionCategory.PREFERRED_LOCATION, "No preferred location among options " + question.getOptions());
        }
        if (Boolean.TRUE.equals(profile.getRelocationWilling())) {
            return Answer.of(QuestionCategory.PREFERRED_LOCATION, "Yes", "relocationWilling=true");
        }
        return Answer.missing(QuestionCategory.PREFERRED_LOCATION, "Location not in preferredLocations and relocation not configured");
    }

    /** Case-insensitive containment match of a value against the option labels. */
    static Optional<String> closestOption(List<String> options, String value) {
        String v = TextNormalizer.normalize(value);
        for (String o : options) {
            String on = TextNormalizer.normalize(o);
            if (on.equals(v)) {
                return Optional.of(o);
            }
        }
        for (String o : options) {
            String on = TextNormalizer.normalize(o);
            if (on.contains(v) || v.contains(on)) {
                return Optional.of(o);
            }
        }
        // Yes/No style option lists
        if (v.equals("yes") || v.equals("no")) {
            for (String o : options) {
                if (TextNormalizer.normalize(o).startsWith(v)) {
                    return Optional.of(o);
                }
            }
        }
        return Optional.empty();
    }

    /** "0-1 years", "1-3 years", "3-5 years", "5+ years", "Less than 1 year", "More than 5 years". */
    static Answer chooseYearsOption(QuestionCategory cat, List<String> options, double years, String source) {
        for (String o : options) {
            double[] range = parseRange(o);
            if (range != null && years >= range[0] && years < range[1] + 1e-9) {
                return Answer.of(cat, o, source + "=" + years + " in option range");
            }
        }
        return Answer.missing(cat, "No option in " + options + " covers " + years + " years");
    }

    static Answer chooseRangeOption(QuestionCategory cat, List<String> options, double value, String source) {
        for (String o : options) {
            double[] range = parseRange(o);
            if (range != null && value >= range[0] && value < range[1] + 1e-9) {
                return Answer.of(cat, o, source + "=" + value + " in option range");
            }
        }
        return Answer.missing(cat, "No option in " + options + " covers " + value);
    }

    static Answer chooseNoticeOption(List<String> options, int days) {
        for (String o : options) {
            String on = TextNormalizer.normalize(o);
            if (days == 0 && (on.contains("immediate") || on.contains("0 days") || on.startsWith("0"))) {
                return Answer.of(QuestionCategory.NOTICE_PERIOD, o, "immediate joiner option");
            }
            double[] range = parseRange(o);
            if (range != null && days >= range[0] && days <= range[1]) {
                return Answer.of(QuestionCategory.NOTICE_PERIOD, o, "noticePeriodDays=" + days + " in option range");
            }
        }
        return Answer.missing(QuestionCategory.NOTICE_PERIOD, "No option in " + options + " matches notice period " + days + " days");
    }

    /** Parses "1-3", "3 to 5", "5+", "more than 5", "less than 1", "upto 30" into [lo, hi]. */
    static double[] parseRange(String option) {
        String o = TextNormalizer.normalize(option);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:-|to)\\s*(\\d+(?:\\.\\d+)?)").matcher(o);
        if (m.find()) {
            return new double[]{Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2))};
        }
        m = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*\\+|more than\\s*(\\d+(?:\\.\\d+)?)|above\\s*(\\d+(?:\\.\\d+)?)|(\\d+(?:\\.\\d+)?)\\s*(?:or more|and above|\\s*yrs? and above)").matcher(o);
        if (m.find()) {
            for (int i = 1; i <= m.groupCount(); i++) {
                if (m.group(i) != null) {
                    return new double[]{Double.parseDouble(m.group(i)), Double.MAX_VALUE};
                }
            }
        }
        m = java.util.regex.Pattern.compile("(?:less than|below|under|upto|up to|within)\\s*(\\d+(?:\\.\\d+)?)").matcher(o);
        if (m.find()) {
            return new double[]{0, Double.parseDouble(m.group(1))};
        }
        return null;
    }
}
