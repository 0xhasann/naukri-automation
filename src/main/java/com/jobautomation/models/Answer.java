package com.jobautomation.models;

import com.jobautomation.questions.QuestionCategory;

/**
 * Result of resolving a question against the profile.
 * Exactly one of: answered (value present), missingData (profile has null for it),
 * unknown (category not recognised), skip (chatbot greeting / non-question).
 */
public final class Answer {
    public enum Outcome { ANSWERED, MISSING_DATA, UNKNOWN, SKIP }

    private final Outcome outcome;
    private final QuestionCategory category;
    private final String value;
    private final String explanation;

    private Answer(Outcome outcome, QuestionCategory category, String value, String explanation) {
        this.outcome = outcome;
        this.category = category;
        this.value = value;
        this.explanation = explanation;
    }

    public static Answer of(QuestionCategory category, String value, String explanation) {
        return new Answer(Outcome.ANSWERED, category, value, explanation);
    }

    public static Answer missing(QuestionCategory category, String explanation) {
        return new Answer(Outcome.MISSING_DATA, category, null, explanation);
    }

    public static Answer unknown(String explanation) {
        return new Answer(Outcome.UNKNOWN, QuestionCategory.UNKNOWN, null, explanation);
    }

    public static Answer skip(QuestionCategory category, String explanation) {
        return new Answer(Outcome.SKIP, category, null, explanation);
    }

    public boolean isAnswered() { return outcome == Outcome.ANSWERED; }
    public boolean isSkipped() { return outcome == Outcome.SKIP; }
    public Outcome getOutcome() { return outcome; }
    public QuestionCategory getCategory() { return category; }
    public String getValue() { return value; }
    public String getExplanation() { return explanation; }

    @Override
    public String toString() {
        return outcome + "/" + category + (value == null ? "" : "=" + value) + " (" + explanation + ")";
    }
}
