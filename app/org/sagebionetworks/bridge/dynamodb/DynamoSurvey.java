package org.sagebionetworks.bridge.dynamodb;

import java.util.List;
import java.util.Objects;

import org.sagebionetworks.bridge.json.DateTimeJsonDeserializer;
import org.sagebionetworks.bridge.json.DateTimeJsonSerializer;
import org.sagebionetworks.bridge.models.GuidCreatedOnVersionHolder;
import org.sagebionetworks.bridge.models.surveys.Survey;
import org.sagebionetworks.bridge.models.surveys.SurveyElement;
import org.sagebionetworks.bridge.models.surveys.SurveyElementConstants;
import org.sagebionetworks.bridge.models.surveys.SurveyElementFactory;
import org.sagebionetworks.bridge.models.surveys.SurveyQuestion;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBVersionAttribute;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

@DynamoDBTable(tableName = "Survey")
public class DynamoSurvey implements Survey {
    private String studyKey;
    private String guid;
    private long createdOn;
    private long modifiedOn;
    private Long version;
    private String name;
    private String identifier;
    private boolean published;
    private boolean deleted;
    private Integer schemaRevision;
    private List<SurveyElement> elements;
    
    public DynamoSurvey() {
        this.elements = Lists.newArrayList();
    }
    
    public DynamoSurvey(String guid, long createdOn) {
        this();
        setGuid(guid);
        setCreatedOn(createdOn);
    }

    /**
     * This copy constructor copies all fields, but it also converts base DynamoSurveyElements to their proper
     * corresponding subclasses (DynamoSurveyQuestion or DynamoSurveyInfoScreen). This is done because Dynamo DB has
     * no concept of inheritance, so we need to re-construct the subclasses.
     */
    public DynamoSurvey(DynamoSurvey survey) {
        this();
        setStudyIdentifier(survey.getStudyIdentifier());
        setGuid(survey.getGuid());
        setCreatedOn(survey.getCreatedOn());
        setModifiedOn(survey.getModifiedOn());
        setVersion(survey.getVersion());
        setName(survey.getName());
        setIdentifier(survey.getIdentifier());
        setPublished(survey.isPublished());
        setDeleted(survey.isDeleted());
        setSchemaRevision(survey.getSchemaRevision());
        for (SurveyElement element : survey.getElements()) {
            elements.add(SurveyElementFactory.fromDynamoEntity(element));
        }
    }
    
    @Override
    @DynamoDBAttribute(attributeName = "studyKey")
    @JsonIgnore
    public String getStudyIdentifier() {
        return studyKey;
    }

    @Override
    public void setStudyIdentifier(String studyKey) {
        this.studyKey = studyKey;
    }

    @Override
    @DynamoDBHashKey
    public String getGuid() {
        return guid;
    }

    @Override
    public void setGuid(String guid) {
        this.guid = guid;
    }

    @Override
    @DynamoDBRangeKey(attributeName="versionedOn")
    @JsonSerialize(using = DateTimeJsonSerializer.class)
    public long getCreatedOn() {
        return createdOn;
    }

    @Override
    @JsonDeserialize(using = DateTimeJsonDeserializer.class)
    public void setCreatedOn(long createdOn) {
        this.createdOn = createdOn;
    }

    @Override
    @DynamoDBVersionAttribute
    public Long getVersion() {
        return version;
    }

    @Override
    public void setVersion(Long version) {
        this.version = version;
    }
    
    @Override
    @DynamoDBAttribute
    @JsonSerialize(using = DateTimeJsonSerializer.class)
    public long getModifiedOn() {
        return modifiedOn;
    }

    @Override
    @JsonDeserialize(using = DateTimeJsonDeserializer.class)
    public void setModifiedOn(long modifiedOn) {
        this.modifiedOn = modifiedOn;
    }

    @Override
    @DynamoDBAttribute
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    @DynamoDBAttribute
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    @DynamoDBAttribute
    public boolean isPublished() {
        return published;
    }

    @Override
    public void setPublished(boolean published) {
        this.published = published;
    }

    @Override
    @DynamoDBAttribute
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
    
    @Override
    public Integer getSchemaRevision() {
        return schemaRevision;
    }

    @Override
    public void setSchemaRevision(Integer schemaRevision) {
        this.schemaRevision = schemaRevision;
    }

    @Override
    @DynamoDBIgnore
    public List<SurveyElement> getElements() {
        return elements;
    }

    @Override
    @DynamoDBIgnore
    @JsonIgnore
    public List<SurveyQuestion> getUnmodifiableQuestionList() {
        ImmutableList.Builder<SurveyQuestion> builder = new ImmutableList.Builder<>();
        for (SurveyElement element : elements) {
            if (SurveyElementConstants.SURVEY_QUESTION_TYPE.equals(element.getType())) {
                builder.add((SurveyQuestion)element);
            }
        }
        return builder.build();
    }

    @Override
    public void setElements(List<SurveyElement> elements) {
        this.elements = elements;
    }
    
    @Override
    public boolean keysEqual(GuidCreatedOnVersionHolder keys) {
        return (keys != null && keys.getGuid().equals(guid) && keys.getCreatedOn() == createdOn);
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Objects.hashCode(studyKey);
        result = prime * result + Objects.hashCode(guid);
        result = prime * result + Objects.hashCode(createdOn);
        result = prime * result + Objects.hashCode(modifiedOn);
        result = prime * result + Objects.hashCode(version);
        result = prime * result + Objects.hashCode(name);
        result = prime * result + Objects.hashCode(identifier);
        result = prime * result + Objects.hashCode(published);
        result = prime * result + Objects.hashCode(deleted);
        result = prime * result + Objects.hashCode(schemaRevision);
        result = prime * result + Objects.hashCode(elements);
        return result;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || !(obj instanceof DynamoSurvey)) {
            return false;
        }
        final DynamoSurvey that = (DynamoSurvey) obj;
        return Objects.equals(this.studyKey, that.studyKey)
                && Objects.equals(this.guid, that.guid)
                && Objects.equals(this.createdOn, that.createdOn)
                && Objects.equals(this.modifiedOn, that.modifiedOn)
                && Objects.equals(this.version, that.version)
                && Objects.equals(this.name, that.name)
                && Objects.equals(this.identifier, that.identifier)
                && Objects.equals(this.published, that.published)
                && Objects.equals(this.deleted, that.deleted)
                && Objects.equals(this.schemaRevision, that.schemaRevision)
                && Objects.equals(this.elements, that.elements);
    }

    @Override
    public String toString() {
        return String.format("DynamoSurvey [studyKey=%s, guid=%s, createdOn=%s, modifiedOn=%s, version=%s, name=%s, identifier=%s, published=%s, deleted=%s, schemaRevision=%s, elements=%s]",
            studyKey, guid, createdOn, modifiedOn, version, name, identifier, published, deleted, schemaRevision, elements);
    }
}
