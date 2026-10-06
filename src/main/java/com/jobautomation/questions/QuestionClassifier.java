package com.jobautomation.questions;

import com.jobautomation.matcher.SkillTaxonomy;
import com.jobautomation.matcher.TextNormalizer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keyword engine: maps a normalised question text to a {@link QuestionCategory}.
 * Order matters - the more specific categories are checked first. Anything that does not
 * match a category is UNKNOWN and will never be answered.
 */
public class QuestionClassifier {

    public static final class Classification {
        public final QuestionCategory category;
        /** For technology questions: the canonical skill the question is about. */
        public final String technology;

        Classification(QuestionCategory category, String technology) {
            this.category = category;
            this.technology = technology;
        }

        @Override
        public String toString() {
            return category + (technology == null ? "" : "(" + technology + ")");
        }
    }

    private static final Map<QuestionCategory, List<String>> KEYWORDS = new LinkedHashMap<>();

    static {
        KEYWORDS.put(QuestionCategory.DATE_OF_BIRTH, List.of("date of birth", "dob", "birth date", "birthdate", "born on"));
        KEYWORDS.put(QuestionCategory.AGE, List.of("your age", "how old are you", "age in years", "current age"));
        KEYWORDS.put(QuestionCategory.GENDER, List.of("gender", "sex"));
        KEYWORDS.put(QuestionCategory.EMAIL, List.of("email", "e-mail", "mail id"));
        KEYWORDS.put(QuestionCategory.PHONE, List.of("phone", "mobile", "contact number", "whatsapp"));
        KEYWORDS.put(QuestionCategory.LINKEDIN, List.of("linkedin"));
        KEYWORDS.put(QuestionCategory.NAME, List.of("full name", "your name", "first name", "last name"));

        KEYWORDS.put(QuestionCategory.EXPECTED_SALARY, List.of("expected salary", "expected ctc", "salary expectation", "expected compensation", "expected package", "expected annual", "salary expectations", "ectc"));
        KEYWORDS.put(QuestionCategory.CURRENT_SALARY, List.of("current salary", "current ctc", "present ctc", "current compensation", "current package", "present salary", "cctc", "current annual"));

        KEYWORDS.put(QuestionCategory.NOTICE_PERIOD, List.of("notice period", "serving notice", "how soon can you join", "joining time", "when can you join", "days to join", "earliest joining", "date of joining", "joining date"));
        KEYWORDS.put(QuestionCategory.IMMEDIATE_JOINER, List.of("immediate joiner", "immediate joining", "available immediately", "join immediately", "immediately available", "join within", "can you join"));

        KEYWORDS.put(QuestionCategory.RELOCATION, List.of("relocat", "willing to move", "open to moving", "shift to", "move to"));
        KEYWORDS.put(QuestionCategory.CURRENT_LOCATION, List.of("current location", "present location", "currently located", "currently based", "where are you located", "current city", "residing", "where do you live", "your location"));
        KEYWORDS.put(QuestionCategory.PREFERRED_LOCATION, List.of("preferred location", "preferred city", "location preference", "which location", "comfortable with the location", "comfortable working in", "comfortable with location", "ok with location", "okay with location", "location is"));

        KEYWORDS.put(QuestionCategory.VISA_SPONSORSHIP, List.of("visa", "sponsorship", "sponsor"));
        KEYWORDS.put(QuestionCategory.WORK_AUTHORIZATION, List.of("work authorization", "authorized to work", "authorised to work", "legally allowed to work", "eligible to work", "work permit", "citizen"));

        KEYWORDS.put(QuestionCategory.WORK_MODE_PREFERENCE, List.of("work from office", "wfo", "hybrid", "remote", "work from home", "onsite", "on-site", "in office", "office 5 days"));
        KEYWORDS.put(QuestionCategory.SHIFT_PREFERENCE, List.of("night shift", "rotational shift", "shift timing", "us shift", "uk shift", "24x7", "weekend"));

        KEYWORDS.put(QuestionCategory.CURRENT_COMPANY, List.of("current company", "current employer", "present company", "current organisation", "current organization", "where do you work"));
        KEYWORDS.put(QuestionCategory.CURRENT_DESIGNATION, List.of("current designation", "current role", "current title", "job title", "designation"));
        KEYWORDS.put(QuestionCategory.HIGHEST_QUALIFICATION, List.of("highest qualification", "highest education", "qualification", "degree", "graduation", "educational background", "education"));

        KEYWORDS.put(QuestionCategory.CONFIRMATION, List.of("i confirm", "do you confirm", "confirm that", "details are true", "information provided is", "acknowledge", "agree to the terms", "consent"));

        // experience families are handled by regex below because of the many phrasings
    }

    private static final Pattern EXPERIENCE_WORDS = Pattern.compile("(years?|yrs?)\\s*(of\\s*)?(experience|exp)|experience\\s*(in\\s*)?(years|yrs)|how many years|total experience|overall experience|professional experience|work experience|relevant experience|hands[- ]on experience|experience with|experience in|experience on|worked (on|with)|do you have experience|are you (skilled|proficient|familiar)|have you worked|knowledge (of|in)|proficien|expertise", Pattern.CASE_INSENSITIVE);
    /** "Do you have Kafka experience?", "Knowledge of Redis?", "Worked on Spring Boot?", "Rate your Java skills". */
    private static final Pattern TECH_CONTEXT = Pattern.compile("experience|knowledge|worked|familiar|proficien|expertise|skilled|hands[- ]on|exposure|comfortable with|rate your", Pattern.CASE_INSENSITIVE);
    private static final Pattern YEARS_WORDS = Pattern.compile("how many years|years?\\b|yrs?\\b|number of years", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOTAL_WORDS = Pattern.compile("total|overall|professional|work experience|it experience|industry experience|how many years of experience do you have|years of experience\\s*\\??$", Pattern.CASE_INSENSITIVE);
    private static final Pattern RELEVANT_WORDS = Pattern.compile("relevant", Pattern.CASE_INSENSITIVE);

    private static final Set<String> NON_TECH_CANONICALS = Set.of("agile", "backend development", "cloud native", "fintech", "payment systems", "data structures", "oop", "system design");

    public Classification classify(String questionText) {
        String q = TextNormalizer.normalize(questionText);
        if (q.isEmpty()) {
            return new Classification(QuestionCategory.UNKNOWN, null);
        }
        if (isInformational(q)) {
            return new Classification(QuestionCategory.INFORMATIONAL, null);
        }

        // 1. Technology-specific experience questions
        Optional<String> tech = technologyIn(q);
        boolean expQuestion = EXPERIENCE_WORDS.matcher(q).find();
        boolean techContext = TECH_CONTEXT.matcher(q).find();
        if (tech.isPresent() && (expQuestion || techContext)) {
            boolean asksYears = YEARS_WORDS.matcher(q).find();
            if ("java".equals(tech.get()) && (RELEVANT_WORDS.matcher(q).find() || asksYears)) {
                return new Classification(QuestionCategory.RELEVANT_JAVA_EXPERIENCE, "java");
            }
            return new Classification(asksYears ? QuestionCategory.TECHNOLOGY_EXPERIENCE_YEARS : QuestionCategory.TECHNOLOGY_YES_NO, tech.get());
        }

        // 2. Keyword categories (specific before generic)
        for (Map.Entry<QuestionCategory, List<String>> e : KEYWORDS.entrySet()) {
            for (String kw : e.getValue()) {
                if (containsKeyword(q, kw)) {
                    return new Classification(e.getKey(), null);
                }
            }
        }

        // 3. Generic experience questions
        if (expQuestion || YEARS_WORDS.matcher(q).find() && q.contains("experience")) {
            if (RELEVANT_WORDS.matcher(q).find()) {
                // "relevant experience" without a named technology -> relevant Java experience (the target role)
                return new Classification(QuestionCategory.RELEVANT_JAVA_EXPERIENCE, "java");
            }
            if (TOTAL_WORDS.matcher(q).find() || q.matches(".*\\b(years|yrs)\\b.*experience.*") || q.matches(".*experience.*\\b(years|yrs)\\b.*")) {
                return new Classification(QuestionCategory.TOTAL_EXPERIENCE, null);
            }
        }
        return new Classification(QuestionCategory.UNKNOWN, null);
    }

    /**
     * Naukri chatbot intros such as "Hi … thank you for showing interest. Kindly answer all the
     * recruiter's questions…" are not screening questions and must not abort the application.
     */
    static boolean isInformational(String normalizedQuestion) {
        String q = normalizedQuestion;
        if (q.contains("thank you for showing interest") || q.contains("thank you for your interest")) {
            return true;
        }
        if (q.contains("kindly answer all") || q.contains("answer all the recruiter")) {
            return true;
        }
        if (q.contains("successfully apply for the job") || q.contains("successfully apply for this job")) {
            return true;
        }
        if (q.contains("please answer the following") && q.contains("question")) {
            return true;
        }
        return (q.startsWith("hi ") || q.startsWith("hello "))
                && q.contains("thank you")
                && q.contains("question");
    }

    /** The most specific technology named in the question, if any. */
    Optional<String> technologyIn(String normalizedQuestion) {
        Set<String> found = SkillTaxonomy.findSkills(normalizedQuestion);
        String best = null;
        for (String s : found) {
            if (NON_TECH_CANONICALS.contains(s)) {
                continue;
            }
            // prefer the longest / most specific mention; "spring boot" over "spring"
            if (best == null || s.length() > best.length()) {
                best = s;
            }
        }
        return Optional.ofNullable(best);
    }

    private static boolean containsKeyword(String q, String kw) {
        if (kw.length() <= 4) {
            // short tokens must match as whole words (avoid "sex" in "sexagenarian", "dob" in "adobe")
            Matcher m = Pattern.compile("(?<![a-z0-9])" + Pattern.quote(kw) + "(?![a-z0-9])").matcher(q);
            return m.find();
        }
        return q.contains(kw);
    }
}
