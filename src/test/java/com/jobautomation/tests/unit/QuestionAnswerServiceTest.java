package com.jobautomation.tests.unit;

import com.jobautomation.config.ProfileLoader;
import com.jobautomation.models.Answer;
import com.jobautomation.models.CandidateProfile;
import com.jobautomation.models.Question;
import com.jobautomation.questions.QuestionAnswerService;
import com.jobautomation.questions.QuestionCategory;
import com.jobautomation.questions.QuestionClassifier;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class QuestionAnswerServiceTest {

    private final CandidateProfile profile = TestFixtures.profile();
    private final QuestionAnswerService service = new QuestionAnswerService(profile);
    private final QuestionClassifier classifier = new QuestionClassifier();

    private static final String MINIMAL_PROFILE = """
            {"name":"T","skills":["Java","Spring Boot","Kafka"],"skillExperienceYears":{"java":3.7,"kafka":3.1,"hibernate":0},
             "totalExperienceYears":3.7,"relocationWilling":true,"immediateJoiner":true,"noticePeriodDays":0,
             "expectedSalaryLpa":22,"preferredLocations":["Bengaluru","Pune"],"workAuthorizationCountries":["India"],
             "requiresVisaSponsorship":false,"currentLocation":"Gurugram","highestQualification":"MCA"}
            """;

    @Test
    public void classifiesRelocationVariants() {
        for (String q : List.of("Are you willing to relocate?", "Would you relocate to Hyderabad?",
                "Open to relocation?", "Are you comfortable relocating to Bangalore?")) {
            Assert.assertEquals(classifier.classify(q).category, QuestionCategory.RELOCATION, q);
        }
    }

    @Test
    public void classifiesExperienceVariants() {
        Assert.assertEquals(classifier.classify("How many years of experience do you have?").category, QuestionCategory.TOTAL_EXPERIENCE);
        Assert.assertEquals(classifier.classify("Total years of experience?").category, QuestionCategory.TOTAL_EXPERIENCE);
        Assert.assertEquals(classifier.classify("Years of professional experience?").category, QuestionCategory.TOTAL_EXPERIENCE);
        Assert.assertEquals(classifier.classify("How many years of relevant experience in Java?").category, QuestionCategory.RELEVANT_JAVA_EXPERIENCE);
        QuestionClassifier.Classification c = classifier.classify("How many years of experience do you have in Kafka?");
        Assert.assertEquals(c.category, QuestionCategory.TECHNOLOGY_EXPERIENCE_YEARS);
        Assert.assertEquals(c.technology, "kafka");
        c = classifier.classify("Do you have experience with Spring Boot?");
        Assert.assertEquals(c.category, QuestionCategory.TECHNOLOGY_YES_NO);
        Assert.assertEquals(c.technology, "spring boot");
    }

    @Test
    public void classifiesOtherCategories() {
        Assert.assertEquals(classifier.classify("Are you an immediate joiner?").category, QuestionCategory.IMMEDIATE_JOINER);
        Assert.assertEquals(classifier.classify("What is your notice period?").category, QuestionCategory.NOTICE_PERIOD);
        Assert.assertEquals(classifier.classify("Expected CTC (in LPA)?").category, QuestionCategory.EXPECTED_SALARY);
        Assert.assertEquals(classifier.classify("What is your current CTC?").category, QuestionCategory.CURRENT_SALARY);
        Assert.assertEquals(classifier.classify("Do you require visa sponsorship?").category, QuestionCategory.VISA_SPONSORSHIP);
        Assert.assertEquals(classifier.classify("Are you authorized to work in India?").category, QuestionCategory.WORK_AUTHORIZATION);
        Assert.assertEquals(classifier.classify("What is your date of birth?").category, QuestionCategory.DATE_OF_BIRTH);
        Assert.assertEquals(classifier.classify("What is your current location?").category, QuestionCategory.CURRENT_LOCATION);
        Assert.assertEquals(classifier.classify("Highest qualification?").category, QuestionCategory.HIGHEST_QUALIFICATION);
    }

    @Test
    public void chatbotGreetingIsSkippedNotUnknown() {
        String greeting = "Hi Mahboob Hasan, thank you for showing interest. Kindly answer all the recruiter's questions to successfully apply for the job.";
        Assert.assertEquals(classifier.classify(greeting).category, QuestionCategory.INFORMATIONAL);
        QuestionAnswerService s = new QuestionAnswerService(profile);
        Answer a = s.answer(Question.text(greeting));
        Assert.assertEquals(a.getOutcome(), Answer.Outcome.SKIP);
        Assert.assertTrue(s.getUnknownQuestions().isEmpty());
        Assert.assertEquals(classifier.classify("Are you willing to relocate?").category, QuestionCategory.RELOCATION);
    }

    @Test
    public void unknownQuestionsAreNeverAnswered() {
        QuestionAnswerService service = new QuestionAnswerService(profile);
        Answer a = service.answer(Question.text("Which programming language are you most passionate about and why?"));
        Assert.assertEquals(a.getOutcome(), Answer.Outcome.UNKNOWN);
        Assert.assertNull(a.getValue());
        Assert.assertEquals(service.getUnknownQuestions().size(), 1);
        a = service.answer(Question.choice("Are you comfortable with night shifts?", List.of("Yes", "No")));
        Assert.assertNotEquals(a.getOutcome(), Answer.Outcome.ANSWERED, "shift preference must not be guessed");
    }

    @Test
    public void relocationAnsweredOnlyFromProfile() {
        CandidateProfile p = ProfileLoader.fromJson(MINIMAL_PROFILE);
        Answer yes = new QuestionAnswerService(p).answer(Question.choice("Are you willing to relocate?", List.of("Yes", "No")));
        Assert.assertEquals(yes.getValue(), "Yes");
        p.setRelocationWilling(false);
        Assert.assertEquals(new QuestionAnswerService(p).answer(Question.choice("Are you willing to relocate?", List.of("Yes", "No"))).getValue(), "No");
        p.setRelocationWilling(null);
        Assert.assertEquals(new QuestionAnswerService(p).answer(Question.text("Are you willing to relocate?")).getOutcome(), Answer.Outcome.MISSING_DATA);
    }

    @Test
    public void experienceUsesVerifiedNumbersNotSearchTarget() {
        CandidateProfile p = ProfileLoader.fromJson(MINIMAL_PROFILE);
        QuestionAnswerService s = new QuestionAnswerService(p);
        Assert.assertEquals(s.answer(Question.text("Total years of experience?")).getValue(), "3.7");
        Assert.assertEquals(s.answer(Question.text("How many years of experience in Kafka?")).getValue(), "3.1");
        Answer opt = s.answer(Question.choice("Total experience?", List.of("0-1 years", "1-3 years", "3-5 years", "5+ years")));
        Assert.assertEquals(opt.getValue(), "3-5 years");
    }

    @Test
    public void technologyNotOnProfileIsNotAnsweredYes() {
        CandidateProfile p = ProfileLoader.fromJson(MINIMAL_PROFILE);
        QuestionAnswerService s = new QuestionAnswerService(p);
        Assert.assertEquals(s.answer(Question.choice("Do you have experience in Kafka?", List.of("Yes", "No"))).getValue(), "Yes");
        Answer k8s = s.answer(Question.choice("Do you have experience in Kubernetes?", List.of("Yes", "No")));
        Assert.assertEquals(k8s.getOutcome(), Answer.Outcome.MISSING_DATA, "unknown tech must stop, not guess");
        // explicitly configured 0 years -> truthful "No"
        Assert.assertEquals(s.answer(Question.choice("Do you have Hibernate experience?", List.of("Yes", "No"))).getValue(), "No");
        Assert.assertEquals(s.answer(Question.text("Years of experience in Hibernate?")).getValue(), "0");
    }

    @Test
    public void dobIsNeverInvented() {
        CandidateProfile p = ProfileLoader.fromJson(MINIMAL_PROFILE);
        Answer a = new QuestionAnswerService(p).answer(Question.text("What is your date of birth?"));
        Assert.assertEquals(a.getOutcome(), Answer.Outcome.MISSING_DATA);
        Assert.assertTrue(a.getExplanation().contains("dob"));
        p.setDob("1997-08-16");
        Assert.assertEquals(new QuestionAnswerService(p).answer(Question.text("Date of birth?")).getValue(), "1997-08-16");
    }

    @Test
    public void salaryAndNoticeUseProfile() {
        CandidateProfile p = ProfileLoader.fromJson(MINIMAL_PROFILE);
        QuestionAnswerService s = new QuestionAnswerService(p);
        Assert.assertEquals(s.answer(Question.text("Expected CTC in LPA?")).getValue(), "22");
        Assert.assertEquals(s.answer(Question.text("Current CTC?")).getOutcome(), Answer.Outcome.MISSING_DATA);
        Assert.assertEquals(s.answer(Question.choice("Notice period?", List.of("Immediate", "15 days", "30 days", "60 days"))).getValue(), "Immediate");
        Assert.assertEquals(s.answer(Question.text("Are you available to join immediately?")).getValue(), "Yes");
    }

    @Test
    public void choiceAnswersMustBeAnOfferedOption() {
        CandidateProfile p = ProfileLoader.fromJson(MINIMAL_PROFILE);
        Answer a = new QuestionAnswerService(p).answer(Question.choice("Highest qualification?", List.of("B.E/B.Tech", "MCA", "M.Tech")));
        Assert.assertEquals(a.getValue(), "MCA");
        Answer none = new QuestionAnswerService(p).answer(Question.choice("Highest qualification?", List.of("B.E/B.Tech", "M.Tech", "PhD")));
        Assert.assertEquals(none.getOutcome(), Answer.Outcome.MISSING_DATA);
    }

    @Test
    public void locationQuestions() {
        CandidateProfile p = ProfileLoader.fromJson(MINIMAL_PROFILE);
        QuestionAnswerService s = new QuestionAnswerService(p);
        Assert.assertEquals(s.answer(Question.text("What is your current location?")).getValue(), "Gurugram");
        Assert.assertEquals(s.answer(Question.choice("Are you comfortable working in Pune?", List.of("Yes", "No"))).getValue(), "Yes");
        Assert.assertEquals(s.answer(Question.choice("Preferred location?", List.of("Mumbai", "Bengaluru", "Kolkata"))).getValue(), "Bengaluru");
    }

    @Test
    public void realProfileFileAnswersCoreQuestions() {
        Assert.assertEquals(service.answer(Question.choice("Do you have Java experience?", List.of("Yes", "No"))).getValue(), "Yes");
        Assert.assertEquals(service.answer(Question.choice("Do you have Spring Boot experience?", List.of("Yes", "No"))).getValue(), "Yes");
        Assert.assertEquals(service.answer(Question.choice("Do you have AWS experience?", List.of("Yes", "No"))).getValue(), "Yes");
        Assert.assertEquals(service.answer(Question.choice("Are you willing to relocate?", List.of("Yes", "No"))).getValue(), "Yes");
        Assert.assertTrue(service.answer(Question.text("Total experience in years?")).isAnswered());
    }
}
