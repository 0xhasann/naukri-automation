package com.jobautomation.exceptions;

import com.jobautomation.models.ApplicationStatus;

/** The Easy Apply form asked something the question engine cannot classify. Never answered blindly. */
public class UnknownQuestionException extends JobSkipException {
    private final String questionText;

    public UnknownQuestionException(String questionText) {
        super(ApplicationStatus.REQUIRES_MANUAL_REVIEW, "Unknown application question: \"" + questionText + "\"");
        this.questionText = questionText;
    }

    public String getQuestionText() { return questionText; }
}
