package org.sagebionetworks.bridge.services;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.sagebionetworks.bridge.validators.Validate.CANNOT_BE_BLANK;
import static org.sagebionetworks.bridge.validators.Validate.CANNOT_BE_NULL;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.bridge.BridgeUtils;
import org.sagebionetworks.bridge.dao.SurveyResponseDao;
import org.sagebionetworks.bridge.dynamodb.DynamoSurveyDao;
import org.sagebionetworks.bridge.models.GuidCreatedOnVersionHolder;
import org.sagebionetworks.bridge.models.GuidCreatedOnVersionHolderImpl;
import org.sagebionetworks.bridge.models.surveys.Survey;
import org.sagebionetworks.bridge.models.surveys.SurveyAnswer;
import org.sagebionetworks.bridge.models.surveys.SurveyQuestion;
import org.sagebionetworks.bridge.models.surveys.SurveyResponse;
import org.sagebionetworks.bridge.models.surveys.SurveyResponseView;
import org.sagebionetworks.bridge.validators.SurveyAnswerValidator;
import org.sagebionetworks.bridge.validators.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.MapBindingResult;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

@Component
public class SurveyResponseServiceImpl implements SurveyResponseService {

    private SurveyResponseDao surveyResponseDao;
    private DynamoSurveyDao surveyDao;
    private TaskEventService taskEventService;

    @Autowired
    public void setSurveyResponseDao(SurveyResponseDao surveyResponseDao) {
        this.surveyResponseDao = surveyResponseDao;
    }

    @Autowired
    public void setSurveyDao(DynamoSurveyDao surveyDao) {
        this.surveyDao = surveyDao;
    }

    @Autowired
    public void setTaskEventService(TaskEventService taskEventService) {
        this.taskEventService = taskEventService;
    }
    
    @Override
    public SurveyResponseView createSurveyResponse(GuidCreatedOnVersionHolder keys, String healthCode, List<SurveyAnswer> answers) {
        return createSurveyResponse(keys, healthCode, answers, BridgeUtils.generateGuid());
    }

    @Override
    public SurveyResponseView createSurveyResponse(GuidCreatedOnVersionHolder keys, String healthCode,
            List<SurveyAnswer> answers, String identifier) {
        checkNotNull(keys, CANNOT_BE_NULL, "keys");
        checkNotNull(answers, CANNOT_BE_NULL, "survey answers");
        checkArgument(isNotBlank(keys.getGuid()), CANNOT_BE_BLANK, "survey guid");
        checkArgument(isNotBlank(identifier), CANNOT_BE_BLANK, "identifier");
        checkArgument(isNotBlank(healthCode), CANNOT_BE_BLANK, "health code");
        checkArgument(keys.getCreatedOn() != 0L, "Survey createdOn cannot be 0");

        Survey survey = surveyDao.getSurvey(keys);
        validate(answers, survey);
        SurveyResponse response = surveyResponseDao.createSurveyResponse(survey, healthCode, answers, identifier);
        fireEvents(survey, response, answers);
        return new SurveyResponseView(response, survey);
    }
    
    @Override
    public SurveyResponseView getSurveyResponse(String healthCode, String identifier) {
        checkNotNull(healthCode, CANNOT_BE_NULL, "health code");
        checkNotNull(identifier, CANNOT_BE_NULL, "identifier");
        
        SurveyResponse response = surveyResponseDao.getSurveyResponse(healthCode, identifier);
        Survey survey = getSurveyForResponse(response);
        return new SurveyResponseView(response, survey);
    }

    @Override
    public SurveyResponseView appendSurveyAnswers(SurveyResponse response, List<SurveyAnswer> answers) {
        checkNotNull(response, CANNOT_BE_NULL, "survey response");
        checkNotNull(answers, CANNOT_BE_NULL, "survey answers");
        
        Survey survey = getSurveyForResponse(response);
        validate(answers, survey);
        SurveyResponse savedResponse = surveyResponseDao.appendSurveyAnswers(response, answers);
        fireEvents(survey, savedResponse, answers);
        return new SurveyResponseView(savedResponse, survey);
    }
    
    @Override
    public void deleteSurveyResponses(String healthCode) {
        checkNotNull(healthCode, CANNOT_BE_NULL, "healthCode");
        
        surveyResponseDao.deleteSurveyResponses(healthCode);
    }

    private Survey getSurveyForResponse(SurveyResponse response) {
        GuidCreatedOnVersionHolder keys = new GuidCreatedOnVersionHolderImpl(response);
        return surveyDao.getSurvey(keys);
    }

    private void validate(List<SurveyAnswer> answers, Survey survey) {
        Map<String, SurveyQuestion> questions = getQuestionsMap(survey.getUnmodifiableQuestionList());
        
        MapBindingResult errors = new MapBindingResult(Maps.newHashMap(), "SurveyResponse");
        for (int i = 0; i < answers.size(); i++) {
            SurveyAnswer answer = answers.get(i);
            SurveyQuestion question = questions.get(answer.getQuestionGuid());
            SurveyAnswerValidator validator = new SurveyAnswerValidator(question);
            Validate.entity(validator, errors, answer);
        }
        Validate.throwException(errors, survey);
    }
    
    private void fireEvents(Survey survey, SurveyResponse response, List<SurveyAnswer> answers) {
        Map<String, SurveyQuestion> questions = getQuestionsMap(survey.getUnmodifiableQuestionList());
        // It's safe to fire an event with the same timestamp more than once. The taskEventDao already 
        // prevents "backtracking" if the timestamp is earlier than the timestamp that's stored.
        for (SurveyAnswer answer : answers) {
            SurveyQuestion question = questions.get(answer.getQuestionGuid());
            if (question != null && question.getFireEvent()) {
                taskEventService.publishEvent(response.getHealthCode(), answer);
            }
        }
        if (response.getStatus() == SurveyResponse.Status.FINISHED) {
            taskEventService.publishEvent(response);
        }
    }

    private Map<String, SurveyQuestion> getQuestionsMap(List<SurveyQuestion> questions) {
        return BridgeUtils.asMap(questions, new Function<SurveyQuestion, String>() {
            public String apply(SurveyQuestion element) {
                return element.getGuid();
            }
        });
    }
    
}