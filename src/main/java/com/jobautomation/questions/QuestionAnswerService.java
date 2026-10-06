package com.jobautomation.questions;

import com.jobautomation.models.Answer;
import com.jobautomation.models.CandidateProfile;
import com.jobautomation.models.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Facade used by the Easy Apply page: classify -> resolve -> validate -> log.
 * It never fills a field unless the answer came from the verified profile.
 */
public class QuestionAnswerService {

    private static final Logger log = LoggerFactory.getLogger(QuestionAnswerService.class);

    private final QuestionClassifier classifier = new QuestionClassifier();
    private final AnswerResolver resolver;
    private final List<String> unknownQuestions = new ArrayList<>();

    public QuestionAnswerService(CandidateProfile profile) {
        this.resolver = new AnswerResolver(profile);
    }

    public Answer answer(Question question) {
        QuestionClassifier.Classification c = classifier.classify(question.getText());
        Answer a = resolver.resolve(question, c);
        if (a.isAnswered()) {
            a = validate(question, a);
        }
        switch (a.getOutcome()) {
            case ANSWERED -> log.info("Q: \"{}\" -> [{}] {}   ({})", question.getText(), c, a.getValue(), a.getExplanation());
            case MISSING_DATA -> log.warn("Q: \"{}\" -> [{}] MISSING DATA: {}", question.getText(), c, a.getExplanation());
            case SKIP -> log.info("Q: \"{}\" -> [{}] skip ({})", question.getText(), c, a.getExplanation());
            default -> {
                unknownQuestions.add(question.getText());
                log.warn("Q: \"{}\" -> UNKNOWN question, will not guess", question.getText());
            }
        }
        return a;
    }

    /** Final guard: an answer for a choice question must be one of the offered options. */
    private Answer validate(Question q, Answer a) {
        if (q.getOptions().isEmpty()) {
            return a;
        }
        for (String o : q.getOptions()) {
            if (o.equalsIgnoreCase(a.getValue())) {
                return a;
            }
        }
        var match = AnswerResolver.closestOption(q.getOptions(), a.getValue());
        if (match.isPresent()) {
            return Answer.of(a.getCategory(), match.get(), a.getExplanation() + " (mapped to option)");
        }
        return Answer.missing(a.getCategory(), "Answer '" + a.getValue() + "' is not one of the options " + q.getOptions());
    }

    public List<String> getUnknownQuestions() {
        return Collections.unmodifiableList(unknownQuestions);
    }

    public QuestionClassifier getClassifier() {
        return classifier;
    }
}
