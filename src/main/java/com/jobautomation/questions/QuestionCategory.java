package com.jobautomation.questions;

/** Normalised categories of Easy Apply questions. UNKNOWN is never answered. */
public enum QuestionCategory {
    RELOCATION,
    IMMEDIATE_JOINER,
    NOTICE_PERIOD,
    TOTAL_EXPERIENCE,
    RELEVANT_JAVA_EXPERIENCE,
    TECHNOLOGY_EXPERIENCE_YEARS,   // "How many years of experience in Kafka?"
    TECHNOLOGY_YES_NO,             // "Do you have experience with Spring Boot?"
    CURRENT_LOCATION,
    PREFERRED_LOCATION,
    WORK_AUTHORIZATION,
    VISA_SPONSORSHIP,
    EXPECTED_SALARY,
    CURRENT_SALARY,
    CURRENT_COMPANY,
    CURRENT_DESIGNATION,
    HIGHEST_QUALIFICATION,
    DATE_OF_BIRTH,
    AGE,
    GENDER,
    NAME,
    EMAIL,
    PHONE,
    LINKEDIN,
    WORK_MODE_PREFERENCE,          // "Are you comfortable working from office?"
    SHIFT_PREFERENCE,              // night shifts / rotational -> not configured -> manual review
    CONFIRMATION,                  // "I confirm the above details are true" style
    INFORMATIONAL,                 // chatbot greeting / "please answer the recruiter's questions" — not a real question
    UNKNOWN
}
