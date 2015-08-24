package org.sagebionetworks.bridge.models.surveys;

import java.util.List;
import java.util.UUID;

import org.sagebionetworks.bridge.TestConstants;
import org.sagebionetworks.bridge.TestUtils;
import org.sagebionetworks.bridge.dynamodb.DynamoSurvey;
import org.sagebionetworks.bridge.dynamodb.DynamoSurveyQuestion;
import org.sagebionetworks.bridge.json.DateUtils;
import org.sagebionetworks.bridge.models.surveys.SurveyRule.Operator;

import com.google.common.collect.Lists;

/**
 * Surveys are complicated. Here's an example survey with nearly every type of question.
 *
 */
public class TestSurvey extends DynamoSurvey {

    public static SurveyQuestion selectBy(Survey survey, DataType type) {
        for (SurveyQuestion question : survey.getUnmodifiableQuestionList()) {
            if (question.getConstraints().getDataType() == type) {
                return question;
            }
        }
        return null;
    }
    
    private DynamoSurveyQuestion multiValueQuestion = new DynamoSurveyQuestion() {
        {
            Image terrible = new Image("http://terrible.svg", 600, 300);
            Image poor = new Image("http://poor.svg", 600, 300);
            Image ok = new Image("http://ok.svg", 600, 300);
            Image good = new Image("http://good.svg", 600, 300);
            Image great = new Image("http://great.svg", 600, 300);
            MultiValueConstraints mvc = new MultiValueConstraints(DataType.INTEGER);
            List<SurveyQuestionOption> options = Lists.newArrayList(
                new SurveyQuestionOption("Terrible", null, "1", terrible),
                new SurveyQuestionOption("Poor", null, "2", poor),
                new SurveyQuestionOption("OK", null, "3", ok),
                new SurveyQuestionOption("Good", null, "4", good),
                new SurveyQuestionOption("Great", null, "5", great)
            );
            mvc.setEnumeration(options);
            mvc.setAllowOther(false);
            mvc.setAllowMultiple(true);
            setConstraints(mvc);
            setPrompt("How do you feel today?");
            setPromptDetail("Is that how you really feel?");
            setIdentifier("feeling");
            setUiHint(UIHint.LIST);
            setGuid(UUID.randomUUID().toString());
        }
    };
    
    private DynamoSurveyQuestion stringQuestion = new DynamoSurveyQuestion() {
        {
            StringConstraints c = new StringConstraints();
            c.setMinLength(2);
            c.setMaxLength(255);
            c.setPattern("\\d{3}-\\d{3}-\\d{4}");
            setPrompt("Please enter an emergency phone number (###-###-####)?");
            setPromptDetail("This should be for someone besides yourself.");
            setIdentifier("name");
            setUiHint(UIHint.TEXTFIELD);
            setConstraints(c);
            setGuid(UUID.randomUUID().toString());
        }
    };
    
    private DynamoSurveyQuestion booleanQuestion = new DynamoSurveyQuestion() {
        {
            BooleanConstraints c = new BooleanConstraints();
            setPrompt("Do you have high blood pressure?");
            setIdentifier("high_bp");
            setPromptDetail("Be honest: do you have high blood pressue?");
            setUiHint(UIHint.CHECKBOX);
            setConstraints(c);
            setGuid(UUID.randomUUID().toString());
        }
    };
    
    private DynamoSurveyQuestion dateQuestion = new DynamoSurveyQuestion() {
        {
            DateConstraints c = new DateConstraints();
            c.setEarliestValue(DateUtils.convertToMillisFromEpoch("2010-10-10T00:00:00.000Z"));
            c.setLatestValue(DateUtils.getCurrentMillisFromEpoch());
            setPrompt("When did you last have a medical check-up?");
            setIdentifier("last_checkup");
            setUiHint(UIHint.DATEPICKER);
            setConstraints(c);
            setGuid(UUID.randomUUID().toString());
        }
    };
    
    private DynamoSurveyQuestion dateTimeQuestion = new DynamoSurveyQuestion() {
        {
            DateTimeConstraints c = new DateTimeConstraints();
            c.setAllowFuture(true);
            c.setEarliestValue(DateUtils.convertToMillisFromEpoch("2010-10-10T00:00:00.000Z"));
            c.setLatestValue(DateUtils.getCurrentMillisFromEpoch());
            setPrompt("When is your next medical check-up scheduled?");
            setIdentifier("last_reading");
            setUiHint(UIHint.DATETIMEPICKER);
            setConstraints(c);
            setGuid(UUID.randomUUID().toString());
        }
    };
    
    private DynamoSurveyQuestion decimalQuestion = new DynamoSurveyQuestion() {
        {
            DecimalConstraints c = new DecimalConstraints();
            c.setMinValue(0.0d);
            c.setMaxValue(10.0d);
            c.setStep(0.1d);
            setPrompt("What dosage (in grams) do you take of deleuterium each day?");
            setIdentifier("deleuterium_dosage");
            setUiHint(UIHint.SLIDER);
            setConstraints(c);
            setGuid(UUID.randomUUID().toString());
        }
    };
    
    private DynamoSurveyQuestion durationQuestion = new DynamoSurveyQuestion() {
        {
            DurationConstraints c = new DurationConstraints();
            setPrompt("How log does your appointment take, on average?");
            setIdentifier("time_for_appt");
            setUiHint(UIHint.NUMBERFIELD);
            setConstraints(c);
            setGuid(UUID.randomUUID().toString());
        }
    };
    
    private DynamoSurveyQuestion integerQuestion = new DynamoSurveyQuestion() {
        {
            IntegerConstraints c = new IntegerConstraints();
            c.setMinValue(0d);
            c.setMaxValue(4d);
            c.getRules().add(new SurveyRule(Operator.LE, 2, "name"));
            c.getRules().add(new SurveyRule(Operator.DE, null, "name"));
            
            setPrompt("How many times a day do you take your blood pressure?");
            setIdentifier("bp_x_day");
            setUiHint(UIHint.NUMBERFIELD);
            setConstraints(c);
            setGuid(UUID.randomUUID().toString());
        }
    };
    
    private DynamoSurveyQuestion timeQuestion = new DynamoSurveyQuestion() {
        {
            TimeConstraints c = new TimeConstraints();
            setPrompt("What times of the day do you take deleuterium?");
            setIdentifier("deleuterium_x_day");
            setUiHint(UIHint.TIMEPICKER);
            setConstraints(c);
            setGuid(UUID.randomUUID().toString());
        }
    };
    
    public TestSurvey(boolean makeNew) {
        setGuid(UUID.randomUUID().toString());
        setName("General Blood Pressure Survey");
        setIdentifier(TestUtils.randomName());
        setModifiedOn(DateUtils.getCurrentMillisFromEpoch());
        setCreatedOn(DateUtils.getCurrentMillisFromEpoch());
        setVersion(2L);
        setPublished(true);
        setSchemaRevision(42);
        setStudyIdentifier(TestConstants.TEST_STUDY_IDENTIFIER);
        List<SurveyElement> elements = getElements();
        elements.add(booleanQuestion);
        elements.add(dateQuestion);
        elements.add(dateTimeQuestion);
        elements.add(decimalQuestion);
        elements.add(integerQuestion);
        elements.add(durationQuestion);
        elements.add(timeQuestion);
        elements.add(multiValueQuestion);
        elements.add(stringQuestion);
        
        if (makeNew) {
            setGuid(null);
            setPublished(false);
            setVersion(null);
            setCreatedOn(0L);
            for (SurveyElement element : getElements()) {
                element.setGuid(null);
            }
        }
    }
    
}
