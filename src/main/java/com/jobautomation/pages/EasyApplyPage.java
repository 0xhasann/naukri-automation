package com.jobautomation.pages;

import com.jobautomation.components.SecurityChallengeDetector;
import com.jobautomation.config.RunMode;
import com.jobautomation.exceptions.JobSkipException;
import com.jobautomation.exceptions.MissingProfileDataException;
import com.jobautomation.exceptions.UnknownQuestionException;
import com.jobautomation.models.Answer;
import com.jobautomation.models.ApplicationStatus;
import com.jobautomation.models.Question;
import com.jobautomation.questions.QuestionAnswerService;
import com.jobautomation.utils.ScreenshotUtil;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Naukri's Easy Apply: clicking Apply either applies instantly (confirmation banner) or opens a
 * chatbot drawer that asks screening questions one at a time; each "Save" advances to the next
 * question and the last Save submits the application.
 * <p>
 * Rules enforced here:
 * <ul>
 *   <li>Every answer comes from {@link QuestionAnswerService}; an UNKNOWN or MISSING_DATA answer
 *       aborts the application (screenshot + question text saved), nothing is guessed.</li>
 *   <li>In TEST / DRY_RUN modes "Save" is never clicked, because any Save may be the final submit.</li>
 *   <li>A security challenge at any point stops the run.</li>
 * </ul>
 */
public class EasyApplyPage extends BasePage {

    public enum Outcome { SUBMITTED, TEST_STOPPED_BEFORE_SUBMIT, NO_CONFIRMATION }

    private static final int MAX_QUESTIONS = 25;
    /** Strict phrases only: a job page can legitimately contain "applied" in other contexts. */
    private static final String[] SUCCESS_MARKERS = {
            "you have successfully applied", "successfully applied to", "application sent successfully",
            "your application has been sent", "your application has been successfully sent", "application submitted successfully"
    };

    private final SecurityChallengeDetector security;
    private final ApplicationConfirmationPage confirmation;
    private final List<String> transcript = new ArrayList<>();

    public EasyApplyPage(WebDriver driver) {
        super(driver);
        this.security = new SecurityChallengeDetector(driver);
        this.confirmation = new ApplicationConfirmationPage(driver);
    }

    public List<String> getTranscript() {
        return transcript;
    }

    public Outcome run(RunMode mode, QuestionAnswerService qa, String jobId) {
        Duration shortT = Duration.ofSeconds(config.getTimeouts().getShort());
        security.assertNoChallenge("easy apply open");

        // Instant apply (no questions)?
        if (waitForEither(shortT)) {
            if (confirmation.isSuccessVisible()) {
                log.info("Applied instantly - confirmation visible");
                return Outcome.SUBMITTED;
            }
        }
        if (!isDrawerVisible()) {
            if (confirmation.isSuccessVisible() || pageMentionsSuccess() || looksAppliedWithoutForm()) {
                log.info("Applied without a question form (instant apply / already-applied marker)");
                return Outcome.SUBMITTED;
            }
            String shot = ScreenshotUtil.capture(driver, "no_apply_form_" + jobId);
            throw new JobSkipException(ApplicationStatus.FAILED, "Neither a confirmation nor a question form appeared after clicking Apply. Screenshot: " + shot);
        }

        String previousQuestion = null;
        for (int i = 1; i <= MAX_QUESTIONS; i++) {
            security.assertNoChallenge("easy apply question " + i);
            if (confirmation.isSuccessVisible() || pageMentionsSuccess()) {
                log.info("Application confirmed after {} question(s)", i - 1);
                return Outcome.SUBMITTED;
            }
            if (!isDrawerVisible()) {
                return confirmation.waitForSuccess(shortT) || looksAppliedWithoutForm()
                        ? Outcome.SUBMITTED : Outcome.NO_CONFIRMATION;
            }

            Question q = readCurrentQuestion();
            if (q == null || q.getText().isBlank() || q.getInputType() == Question.InputType.UNKNOWN) {
                // Drawer open but no answerable question rendered yet (greeting / loading) - give it a moment
                String current = q == null ? previousQuestion : q.getText();
                if (!waitForInputControls(shortT) && !waitForQuestionChange(current, shortT)) {
                    String shot = ScreenshotUtil.capture(driver, "no_question_rendered_" + jobId);
                    throw new JobSkipException(ApplicationStatus.FAILED, "Easy Apply drawer open but no question rendered. Screenshot: " + shot);
                }
                continue;
            }

            Answer a = qa.answer(q);
            if (a.isSkipped()) {
                transcript.add("Q: " + q.getText() + " -> SKIP (" + a.getExplanation() + ")");
                if (q.getText().equals(previousQuestion)) {
                    String shot = ScreenshotUtil.capture(driver, "greeting_not_advancing_" + jobId);
                    throw new JobSkipException(ApplicationStatus.FAILED,
                            "Chatbot greeting did not advance to a real question. Screenshot: " + shot);
                }
                advancePastInformational(mode, q.getText(), shortT);
                previousQuestion = q.getText();
                continue;
            }
            if (q.getText().equals(previousQuestion)) {
                String shot = ScreenshotUtil.capture(driver, "question_not_advancing_" + jobId);
                throw new JobSkipException(ApplicationStatus.FAILED, "Form did not advance after saving '" + q.getText() + "'. Screenshot: " + shot);
            }
            log.info("Question {}: {}", i, q);

            if (!a.isAnswered()) {
                String shot = ScreenshotUtil.capture(driver, "unanswerable_question_" + jobId);
                transcript.add("Q: " + q.getText() + " -> " + a.getOutcome() + " (" + a.getExplanation() + ")");
                if (a.getOutcome() == Answer.Outcome.MISSING_DATA) {
                    throw new MissingProfileDataException("Required information missing for question '" + q.getText() + "': " + a.getExplanation() + ". Screenshot: " + shot);
                }
                throw new UnknownQuestionException(q.getText());
            }
            transcript.add("Q: " + q.getText() + " -> " + a.getValue());

            fill(q, a);
            if (!mode.submits()) {
                log.info("{} MODE: filled '{}' for '{}' but NOT saving/submitting", mode, a.getValue(), q.getText());
                ScreenshotUtil.capture(driver, "test_mode_filled_" + jobId);
                closeDrawer();
                return Outcome.TEST_STOPPED_BEFORE_SUBMIT;
            }
            click("easyApply.saveButton");
            previousQuestion = q.getText();
            waitForQuestionChange(previousQuestion, Duration.ofSeconds(config.getTimeouts().getExplicit()));
        }
        String shot = ScreenshotUtil.capture(driver, "too_many_questions_" + jobId);
        throw new JobSkipException(ApplicationStatus.REQUIRES_MANUAL_REVIEW, "More than " + MAX_QUESTIONS + " questions; stopping for manual review. Screenshot: " + shot);
    }

    // ------------------------------------------------------------------ reading

    boolean isDrawerVisible() {
        return isDisplayed("easyApply.chatDrawer");
    }

    Question readCurrentQuestion() {
        String text = "";
        List<WebElement> msgs = findAll("easyApply.botMessage");
        if (!msgs.isEmpty()) {
            text = safeText(msgs.get(msgs.size() - 1));
        }
        if (text.isBlank()) {
            text = getText("easyApply.lastBotMessage");
        }
        List<String> radio = labels("easyApply.radioOptions");
        if (!radio.isEmpty()) {
            return new Question(text, Question.InputType.SINGLE_CHOICE, radio, true);
        }
        List<String> checks = labels("easyApply.checkboxOptions");
        if (!checks.isEmpty()) {
            return new Question(text, Question.InputType.MULTI_CHOICE, checks, true);
        }
        List<String> chips = labels("easyApply.suggestionChips");
        if (!chips.isEmpty()) {
            return new Question(text, Question.InputType.SINGLE_CHOICE, chips, true);
        }
        if (isDisplayed("easyApply.dropdown")) {
            List<String> opts = new ArrayList<>();
            find("easyApply.dropdown").ifPresent(sel -> {
                for (WebElement o : sel.findElements(org.openqa.selenium.By.tagName("option"))) {
                    String t = safeText(o);
                    if (!t.isBlank() && !t.toLowerCase(Locale.ROOT).startsWith("select")) {
                        opts.add(t);
                    }
                }
            });
            return new Question(text, Question.InputType.DROPDOWN, opts, true);
        }
        if (isDisplayed("easyApply.textInput")) {
            return new Question(text, Question.InputType.TEXT, null, true);
        }
        return new Question(text, Question.InputType.UNKNOWN, null, true);
    }

    private List<String> labels(String key) {
        List<String> out = new ArrayList<>();
        for (WebElement e : findAll(key)) {
            try {
                if (e.isDisplayed()) {
                    String t = safeText(e);
                    if (!t.isBlank()) {
                        out.add(t);
                    }
                }
            } catch (StaleElementReferenceException ignored) {
                // re-rendered; skip
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ filling

    private void fill(Question q, Answer a) {
        switch (q.getInputType()) {
            case SINGLE_CHOICE, MULTI_CHOICE -> clickOption(q, a.getValue());
            case DROPDOWN -> {
                WebElement sel = waitForVisible("easyApply.dropdown");
                new org.openqa.selenium.support.ui.Select(sel).selectByVisibleText(a.getValue());
            }
            case TEXT -> type("easyApply.textInput", a.getValue());
            default -> {
                String shot = ScreenshotUtil.capture(driver, "unknown_input_type");
                throw new JobSkipException(ApplicationStatus.REQUIRES_MANUAL_REVIEW,
                        "Unsupported input type for question '" + q.getText() + "'. Screenshot: " + shot);
            }
        }
    }

    private void clickOption(Question q, String value) {
        String[] keys = {"easyApply.radioOptions", "easyApply.checkboxOptions", "easyApply.suggestionChips"};
        for (String key : keys) {
            for (WebElement e : findAll(key)) {
                try {
                    if (e.isDisplayed() && safeText(e).equalsIgnoreCase(value)) {
                        click(e);
                        return;
                    }
                } catch (StaleElementReferenceException ignored) {
                    // list re-rendered; continue scanning
                }
            }
        }
        throw new JobSkipException(ApplicationStatus.FAILED, "Option '" + value + "' not found among " + q.getOptions());
    }

    // ------------------------------------------------------------------ waiting

    /** Waits until either a confirmation or the question drawer is visible. */
    private boolean waitForEither(Duration timeout) {
        try {
            return fluentWait(timeout).until(d -> confirmation.isSuccessVisible() || isDrawerVisible() || pageMentionsSuccess());
        } catch (TimeoutException e) {
            return false;
        }
    }

    private boolean waitForQuestionChange(String previous, Duration timeout) {
        try {
            return fluentWait(timeout).until(d -> {
                if (confirmation.isSuccessVisible() || pageMentionsSuccess() || !isDrawerVisible()) {
                    return true;
                }
                Question q = readCurrentQuestion();
                return q != null && !q.getText().isBlank() && !q.getText().equals(previous);
            });
        } catch (TimeoutException e) {
            return false;
        }
    }

    /** Waits until some input control (options, text box, dropdown) is rendered for the current question. */
    private boolean waitForInputControls(Duration timeout) {
        try {
            return fluentWait(timeout).until(d -> {
                Question q = readCurrentQuestion();
                return q != null && !q.getText().isBlank() && q.getInputType() != Question.InputType.UNKNOWN;
            });
        } catch (TimeoutException e) {
            return false;
        }
    }

    private boolean pageMentionsSuccess() {
        String body = pageTextLower();
        for (String m : SUCCESS_MARKERS) {
            if (body.contains(m)) {
                return true;
            }
        }
        return false;
    }

    void closeDrawer() {
        try {
            if (isDisplayed("easyApply.closeButton")) {
                click("easyApply.closeButton");
            }
        } catch (RuntimeException e) {
            log.debug("Could not close drawer: {}", e.getMessage());
        }
    }

    /**
     * Greeting bubbles are not questions. Wait for the next bot message; in LIVE, click Save/Send
     * once if Naukri requires that to reveal the first real question.
     */
    private void advancePastInformational(RunMode mode, String greetingText, Duration timeout) {
        if (waitForQuestionChange(greetingText, timeout)) {
            return;
        }
        if (!mode.submits()) {
            return;
        }
        if (isDisplayed("easyApply.saveButton")) {
            log.info("Advancing past chatbot greeting (Save/Send)");
            click("easyApply.saveButton");
            waitForQuestionChange(greetingText, timeout);
        }
    }

    /** Instant apply with no chatbot: Apply disappears or Naukri shows Already Applied. */
    private boolean looksAppliedWithoutForm() {
        return isDisplayed("details.alreadyApplied") || !isDisplayed("details.easyApplyButton");
    }
}
