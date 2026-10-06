package com.jobautomation.models;

import java.util.ArrayList;
import java.util.List;

/** A question rendered by the Easy Apply chatbot/form. */
public class Question {
    public enum InputType { TEXT, SINGLE_CHOICE, MULTI_CHOICE, DROPDOWN, UNKNOWN }

    private final String text;
    private final InputType inputType;
    private final List<String> options;
    private final boolean mandatory;

    public Question(String text, InputType inputType, List<String> options, boolean mandatory) {
        this.text = text == null ? "" : text.trim();
        this.inputType = inputType;
        this.options = options == null ? new ArrayList<>() : options;
        this.mandatory = mandatory;
    }

    public static Question text(String text) {
        return new Question(text, InputType.TEXT, null, true);
    }

    public static Question choice(String text, List<String> options) {
        return new Question(text, InputType.SINGLE_CHOICE, options, true);
    }

    public String getText() { return text; }
    public InputType getInputType() { return inputType; }
    public List<String> getOptions() { return options; }
    public boolean isMandatory() { return mandatory; }

    @Override
    public String toString() {
        return "Q[" + inputType + "] " + text + (options.isEmpty() ? "" : " options=" + options);
    }
}
