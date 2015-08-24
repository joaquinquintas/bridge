package org.sagebionetworks.bridge.dynamodb;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.sagebionetworks.bridge.BridgeUtils;
import org.sagebionetworks.bridge.dao.SurveyDao;
import org.sagebionetworks.bridge.exceptions.BridgeServiceException;
import org.sagebionetworks.bridge.exceptions.ConcurrentModificationException;
import org.sagebionetworks.bridge.exceptions.EntityNotFoundException;
import org.sagebionetworks.bridge.exceptions.PublishedSurveyException;
import org.sagebionetworks.bridge.json.DateUtils;
import org.sagebionetworks.bridge.models.GuidCreatedOnVersionHolder;
import org.sagebionetworks.bridge.models.studies.StudyIdentifier;
import org.sagebionetworks.bridge.models.surveys.Survey;
import org.sagebionetworks.bridge.models.surveys.SurveyElement;
import org.sagebionetworks.bridge.models.surveys.SurveyElementFactory;
import org.springframework.stereotype.Component;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper.FailedBatch;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.QueryResultPage;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Component
public class DynamoSurveyDao implements SurveyDao {

    Comparator<DynamoSurvey> VERSIONED_ON_DESC_SORTER = new Comparator<DynamoSurvey>() {
        @Override public int compare(DynamoSurvey o1, DynamoSurvey o2) {
            return (int)(o2.getCreatedOn() - o1.getCreatedOn());
        }
    };
    
    class QueryBuilder {
        
        private static final String PUBLISHED_PROPERTY = "published";
        private static final String DELETED_PROPERTY = "deleted";
        private static final String CREATED_ON_PROPERTY = "versionedOn";
        private static final String STUDY_KEY_PROPERTY = "studyKey";
        private static final String IDENTIFIER_PROPERTY = "identifier";
        
        String surveyGuid;
        String studyIdentifier;
        String identifier;
        long createdOn;
        boolean published;
        boolean notDeleted;
        
        QueryBuilder setSurvey(String surveyGuid) {
            this.surveyGuid = surveyGuid;
            return this;
        }
        QueryBuilder setStudy(StudyIdentifier studyIdentifier) {
            this.studyIdentifier = studyIdentifier.getIdentifier();
            return this;
        }
        QueryBuilder setIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }
        QueryBuilder setCreatedOn(long createdOn) {
            this.createdOn = createdOn;
            return this;
        }
        QueryBuilder isPublished() {
            this.published = true;
            return this;
        }
        QueryBuilder isNotDeleted() {
            this.notDeleted = true;
            return this;
        }
        
        int getCount() {
            DynamoSurvey key = new DynamoSurvey();
            key.setGuid(surveyGuid);
            DynamoDBQueryExpression<DynamoSurvey> query = new DynamoDBQueryExpression<DynamoSurvey>();
            query.withScanIndexForward(false);
            query.withHashKeyValues(key);    
            if (studyIdentifier != null) {
                query.withQueryFilterEntry(STUDY_KEY_PROPERTY, equalsString(studyIdentifier));
            }
            if (createdOn != 0L) {
                query.withRangeKeyCondition(CREATED_ON_PROPERTY, equalsNumber(Long.toString(createdOn)));
            }
            if (published) {
                query.withQueryFilterEntry(PUBLISHED_PROPERTY, equalsNumber("1"));
            }
            if (notDeleted) {
                query.withQueryFilterEntry(DELETED_PROPERTY, equalsNumber("0"));
            }
            return surveyMapper.queryPage(DynamoSurvey.class, query).getCount();
        }
        
        List<Survey> getAll(boolean exceptionIfEmpty) {
            List<DynamoSurvey> dynamoSurveys = null;
            if (surveyGuid == null) {
                dynamoSurveys = scan();
            } else {
                dynamoSurveys = query();
            }
            if (exceptionIfEmpty && dynamoSurveys.size() == 0) {
                throw new EntityNotFoundException(DynamoSurvey.class);
            }
            List<Survey> surveys = Lists.newArrayListWithCapacity(dynamoSurveys.size());
            for (DynamoSurvey s : dynamoSurveys) {
                surveys.add((Survey)s);
            }
            return surveys;
        }
        
        Survey getOne(boolean exceptionIfEmpty) {
            List<Survey> surveys = getAll(exceptionIfEmpty);
            if (!surveys.isEmpty()) {
                attachSurveyElements(surveys.get(0));
                return surveys.get(0);
            }
            return null;
        }

        private List<DynamoSurvey> query() {
            DynamoDBQueryExpression<DynamoSurvey> query = new DynamoDBQueryExpression<DynamoSurvey>();
            query.withScanIndexForward(false);
            query.withHashKeyValues(new DynamoSurvey(surveyGuid, createdOn));    
            if (studyIdentifier != null) {
                query.withQueryFilterEntry(STUDY_KEY_PROPERTY, equalsString(studyIdentifier));
            }
            if (createdOn != 0L) {
                query.withRangeKeyCondition(CREATED_ON_PROPERTY, equalsNumber(Long.toString(createdOn)));
            }
            if (published) {
                query.withQueryFilterEntry(PUBLISHED_PROPERTY, equalsNumber("1"));
            }
            if (notDeleted) {
                query.withQueryFilterEntry(DELETED_PROPERTY, equalsNumber("0"));
            }
            return surveyMapper.queryPage(DynamoSurvey.class, query).getResults();
        }

        private List<DynamoSurvey> scan() {
            DynamoDBScanExpression scan = new DynamoDBScanExpression();
            if (studyIdentifier != null) {
                scan.addFilterCondition(STUDY_KEY_PROPERTY, equalsString(studyIdentifier));
            }
            if (createdOn != 0L) {
                scan.addFilterCondition(CREATED_ON_PROPERTY, equalsNumber(Long.toString(createdOn)));
            }
            if (published) {
                scan.addFilterCondition(PUBLISHED_PROPERTY, equalsNumber("1"));
            }
            if (identifier != null) {
                scan.addFilterCondition(IDENTIFIER_PROPERTY, equalsString(identifier));
            }
            if (notDeleted) {
                scan.addFilterCondition(DELETED_PROPERTY, equalsNumber("0"));
            }
            // Scans will not sort as queries do. Sort Manually.
            List<DynamoSurvey> surveys = Lists.newArrayList(surveyMapper.scan(DynamoSurvey.class, scan));
            Collections.sort(surveys, VERSIONED_ON_DESC_SORTER);
            return surveys;
        }

        private Condition equalsNumber(String equalTo) {
            Condition condition = new Condition();
            condition.withComparisonOperator(ComparisonOperator.EQ);
            condition.withAttributeValueList(new AttributeValue().withN(equalTo));
            return condition;
        }
        
        private Condition equalsString(String equalTo) {
            Condition condition = new Condition();
            condition.withComparisonOperator(ComparisonOperator.EQ);
            condition.withAttributeValueList(new AttributeValue().withS(equalTo));
            return condition;
        }
        
        private void attachSurveyElements(Survey survey) {
            DynamoSurveyElement template = new DynamoSurveyElement();
            template.setSurveyKeyComponents(survey.getGuid(), survey.getCreatedOn());
            
            DynamoDBQueryExpression<DynamoSurveyElement> query = new DynamoDBQueryExpression<DynamoSurveyElement>();
            query.withHashKeyValues(template);
            
            QueryResultPage<DynamoSurveyElement> page = surveyElementMapper.queryPage(DynamoSurveyElement.class, query);

            List<SurveyElement> elements = Lists.newArrayList();
            for (DynamoSurveyElement element : page.getResults()) {
                elements.add(SurveyElementFactory.fromDynamoEntity(element));
            }
            survey.setElements(elements);
        }
    }

    private DynamoDBMapper surveyMapper;
    private DynamoDBMapper surveyElementMapper;

    @Resource(name = "surveyMapper")
    public void setSurveyMapper(DynamoDBMapper surveyMapper) {
        this.surveyMapper = surveyMapper;
    }
    
    @Resource(name = "surveyElementMapper")
    public void setSurveyElementMapper(DynamoDBMapper surveyElementMapper) {
        this.surveyElementMapper = surveyElementMapper;
    }

    @Override
    public Survey createSurvey(Survey survey) {
        checkNotNull(survey.getStudyIdentifier(), "Survey study identifier is null");
        if (survey.getGuid() == null) {
            survey.setGuid(BridgeUtils.generateGuid());
        }
        long time = DateUtils.getCurrentMillisFromEpoch();
        survey.setCreatedOn(time);
        survey.setModifiedOn(time);
        survey.setPublished(false);
        survey.setDeleted(false);
        return saveSurvey(survey);
    }

    @Override
    public Survey publishSurvey(GuidCreatedOnVersionHolder keys) {
        Survey survey = getSurvey(keys);
        if (survey.isDeleted()) {
            throw new EntityNotFoundException(Survey.class);
        }
        if (!survey.isPublished()) {
            survey.setPublished(true);
            survey.setModifiedOn(DateUtils.getCurrentMillisFromEpoch());
            try {
                surveyMapper.save(survey);
            } catch(ConditionalCheckFailedException e) {
                throw new ConcurrentModificationException(survey);
            }
        }
        return survey;
    }
    
    @Override
    public Survey updateSurvey(Survey survey) {
        Survey existing = getSurvey(survey);
        if (existing.isDeleted()) {
            throw new EntityNotFoundException(Survey.class);
        }
        if (existing.isPublished()) {
            throw new PublishedSurveyException(survey);
        }
        existing.setIdentifier(survey.getIdentifier());
        existing.setName(survey.getName());
        existing.setElements(survey.getElements());
        existing.setModifiedOn(DateUtils.getCurrentMillisFromEpoch());
        
        return saveSurvey(survey);
    }
    
    @Override
    public Survey versionSurvey(GuidCreatedOnVersionHolder keys) {
        DynamoSurvey existing = (DynamoSurvey)getSurvey(keys);
        if (existing.isDeleted()) {
            throw new EntityNotFoundException(Survey.class);
        }
        DynamoSurvey copy = new DynamoSurvey(existing);
        copy.setPublished(false);
        copy.setDeleted(false);
        copy.setVersion(null);
        long time = DateUtils.getCurrentMillisFromEpoch();
        copy.setCreatedOn(time);
        copy.setModifiedOn(time);
        for (SurveyElement element : copy.getElements()) {
            element.setGuid(BridgeUtils.generateGuid());
        }
        return saveSurvey(copy);
    }

    @Override
    public void deleteSurvey(GuidCreatedOnVersionHolder keys) {
        Survey existing = getSurvey(keys);
        if (existing.isDeleted()) {
            throw new EntityNotFoundException(Survey.class);
        }
        // If a survey has been published, you can't delete the last published version of that survey.
        // This is going to create a lot of test errors.
        if (existing.isPublished()) {
            int publishedVersionCount = new QueryBuilder().setSurvey(keys.getGuid()).isPublished().isNotDeleted().getCount();
            if (publishedVersionCount < 2) {
                throw new PublishedSurveyException(existing, "You cannot delete the last published version of a published survey.");
            }
        }
        existing.setDeleted(true);
        saveSurvey(existing);
    }

    @Override
    public List<Survey> getSurveyAllVersions(StudyIdentifier studyIdentifier, String guid) {
        return new QueryBuilder().setStudy(studyIdentifier).setSurvey(guid).isNotDeleted().getAll(true);
    }
    
    @Override
    public Survey getSurveyMostRecentVersion(StudyIdentifier studyIdentifier, String guid) {
        return new QueryBuilder().setStudy(studyIdentifier).setSurvey(guid).isNotDeleted().getOne(true);
    }

    @Override
    public Survey getSurveyMostRecentlyPublishedVersion(StudyIdentifier studyIdentifier, String guid) {
        return new QueryBuilder().setStudy(studyIdentifier).isPublished().setSurvey(guid).isNotDeleted().getOne(true);
    }
    
    @Override
    public Survey getSurveyMostRecentlyPublishedVersionByIdentifier(StudyIdentifier studyIdentifier, String identifier) {
        return new QueryBuilder().setStudy(studyIdentifier).setIdentifier(identifier).isPublished().isNotDeleted().getOne(true);
    }
    
    @Override
    public List<Survey> getAllSurveysMostRecentlyPublishedVersion(StudyIdentifier studyIdentifier) {
        return new QueryBuilder().setStudy(studyIdentifier).isPublished().isNotDeleted().getAll(false);
    }
    
    @Override
    public List<Survey> getAllSurveysMostRecentVersion(StudyIdentifier studyIdentifier) {
        List<Survey> surveys = new QueryBuilder().setStudy(studyIdentifier).isNotDeleted().getAll(false);
        if (surveys.isEmpty()) {
            return surveys;
        }
        // If you knew the number of unique guids, you could iterate until you had found
        // that many unique GUIDs, and stop, since they're ordered from largest timestamp 
        // to smaller. This would be faster with many versions to go through.
        Map<String, Survey> map = Maps.newLinkedHashMap();
        for (Survey survey : surveys) {
            Survey stored = map.get(survey.getGuid());
            if (stored == null || survey.getCreatedOn() > stored.getCreatedOn()) {
                map.put(survey.getGuid(), survey);
            }
        }
        return new ArrayList<Survey>(map.values());
    }
    
    /**
     * Get a specific survey version regardless of whether or not is has been deleted. This is the only call 
     * that will return a deleted survey. With most scheduling now pointing to the most recently published 
     * version (not a specific timestamped version), this method should be rarely called.
     */
    @Override
    public Survey getSurvey(GuidCreatedOnVersionHolder keys) {
        return new QueryBuilder().setSurvey(keys.getGuid()).setCreatedOn(keys.getCreatedOn()).getOne(true);
    }
    
    private Survey saveSurvey(Survey survey) {
        deleteAllElements(survey.getGuid(), survey.getCreatedOn());
        
        List<DynamoSurveyElement> dynamoElements = Lists.newArrayList();
        for (int i=0; i < survey.getElements().size(); i++) {
            SurveyElement element = survey.getElements().get(i);
            element.setSurveyKeyComponents(survey.getGuid(), survey.getCreatedOn());
            element.setOrder(i);
            if (element.getGuid() == null) {
                element.setGuid(BridgeUtils.generateGuid());
            }
            dynamoElements.add((DynamoSurveyElement)element);
        }
        
        List<FailedBatch> failures = surveyElementMapper.batchSave(dynamoElements);
        BridgeUtils.ifFailuresThrowException(failures);
        
        try {
            surveyMapper.save(survey);    
        } catch(ConditionalCheckFailedException throwable) {
            throw new ConcurrentModificationException(survey);
        } catch(Throwable t) {
            throw new BridgeServiceException(t);
        }
        return survey;
    }
    
    private void deleteAllElements(String surveyGuid, long createdOn) {
        DynamoSurveyElement template = new DynamoSurveyElement();
        template.setSurveyKeyComponents(surveyGuid, createdOn);
        
        DynamoDBQueryExpression<DynamoSurveyElement> query = new DynamoDBQueryExpression<DynamoSurveyElement>();
        query.withHashKeyValues(template);
        
        QueryResultPage<DynamoSurveyElement> page = surveyElementMapper.queryPage(DynamoSurveyElement.class, query);
        List<FailedBatch> failures = surveyElementMapper.batchDelete(page.getResults());
        BridgeUtils.ifFailuresThrowException(failures);
    }
}
