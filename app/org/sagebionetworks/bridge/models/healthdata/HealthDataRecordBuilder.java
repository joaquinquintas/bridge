package org.sagebionetworks.bridge.models.healthdata;

import com.fasterxml.jackson.databind.JsonNode;
import org.joda.time.LocalDate;

import org.sagebionetworks.bridge.dao.ParticipantOption;
import org.sagebionetworks.bridge.json.BridgeObjectMapper;
import org.sagebionetworks.bridge.json.DateUtils;
import org.sagebionetworks.bridge.validators.HealthDataRecordValidator;
import org.sagebionetworks.bridge.validators.Validate;

/**
 * This class is used so that whenever another class (such as the upload validation worker apps) needs to create a
 * prototype HealthDataRecord object to save into DynamoDB, this class can be used instead of using the Dynamo
 * implementation directly.
 */
public abstract class HealthDataRecordBuilder {
    private Long createdOn;
    private JsonNode data;
    private String healthCode;
    private String id;
    private JsonNode metadata;
    private String schemaId;
    private int schemaRevision;
    private String studyId;
    private LocalDate uploadDate;
    private String uploadId;
    private ParticipantOption.SharingScope userSharingScope;
    private String userExternalId;
    private Long version;

    /** Copies all fields from the specified record into the builder. This is useful for updating records. */
    public HealthDataRecordBuilder copyOf(HealthDataRecord record) {
        createdOn = record.getCreatedOn();
        data = record.getData();
        healthCode = record.getHealthCode();
        id = record.getId();
        metadata = record.getMetadata();
        schemaId = record.getSchemaId();
        schemaRevision = record.getSchemaRevision();
        studyId = record.getStudyId();
        uploadDate = record.getUploadDate();
        uploadId = record.getUploadId();
        userSharingScope = record.getUserSharingScope();
        version = record.getVersion();
        return this;
    }

    /** @see org.sagebionetworks.bridge.models.healthdata.HealthDataRecord#getCreatedOn */
    public Long getCreatedOn() {
        return createdOn;
    }

    /** @see org.sagebionetworks.bridge.models.healthdata.HealthDataRecord#getCreatedOn */
    public HealthDataRecordBuilder withCreatedOn(Long createdOn) {
        this.createdOn = createdOn;
        return this;
    }

    /** @see org.sagebionetworks.bridge.models.healthdata.HealthDataRecord#getData */
    public JsonNode getData() {
        return data;
    }

    /** @see org.sagebionetworks.bridge.models.healthdata.HealthDataRecord#getData */
    public HealthDataRecordBuilder withData(JsonNode data) {
        this.data = data;
        return this;
    }

    /** @see org.sagebionetworks.bridge.models.healthdata.HealthDataRecord#getHealthCode */
    public String getHealthCode() {
        return healthCode;
    }

    /** @see org.sagebionetworks.bridge.models.healthdata.HealthDataRecord#getHealthCode */
    public HealthDataRecordBuilder withHealthCode(String healthCode) {
        this.healthCode = healthCode;
        return this;
    }

    /** @see org.sagebionetworks.bridge.models.healthdata.HealthDataRecord#getId */
    public String getId() {
        return id;
    }

    /** @see org.sagebionetworks.bridge.models.healthdata.HealthDataRecord#getId */
    public HealthDataRecordBuilder withId(String id) {
        this.id = id;
        return this;
    }

    /** @see org.sagebionetworks.bridge.models.healthdata.HealthDataRecord#getMetadata */
    public JsonNode getMetadata() {
        return metadata;
    }

    /** @see org.sagebionetworks.bridge.models.healthdata.HealthDataRecord#getMetadata */
    public HealthDataRecordBuilder withMetadata(JsonNode metadata) {
        this.metadata = metadata;
        return this;
    }

    /** @see org.sagebionetworks.bridge.models.healthdata.HealthDataRecord#getSchemaId */
    public String getSchemaId() {
        return schemaId;
    }

    /** @see org.sagebionetworks.bridge.models.healthdata.HealthDataRecord#getSchemaId */
    public HealthDataRecordBuilder withSchemaId(String schemaId) {
        this.schemaId = schemaId;
        return this;
    }

    /** @see org.sagebionetworks.bridge.models.healthdata.HealthDataRecord#getSchemaRevision */
    public int getSchemaRevision() {
        return schemaRevision;
    }

    /** @see org.sagebionetworks.bridge.models.healthdata.HealthDataRecord#getSchemaRevision */
    public HealthDataRecordBuilder withSchemaRevision(int schemaRevision) {
        this.schemaRevision = schemaRevision;
        return this;
    }

    /** @see org.sagebionetworks.bridge.models.healthdata.HealthDataRecord#getStudyId */
    public String getStudyId() {
        return studyId;
    }

    /** @see org.sagebionetworks.bridge.models.healthdata.HealthDataRecord#getStudyId */
    public HealthDataRecordBuilder withStudyId(String studyId) {
        this.studyId = studyId;
        return this;
    }

    /** @see org.sagebionetworks.bridge.models.healthdata.HealthDataRecord#getUploadDate */
    public LocalDate getUploadDate() {
        return uploadDate;
    }

    /** @see org.sagebionetworks.bridge.models.healthdata.HealthDataRecord#getUploadDate */
    public HealthDataRecordBuilder withUploadDate(LocalDate uploadDate) {
        this.uploadDate = uploadDate;
        return this;
    }

    /** @see org.sagebionetworks.bridge.models.healthdata.HealthDataRecord#getUploadId */
    public String getUploadId() {
        return uploadId;
    }

    /** @see org.sagebionetworks.bridge.models.healthdata.HealthDataRecord#getUploadId */
    public HealthDataRecordBuilder withUploadId(String uploadId) {
        this.uploadId = uploadId;
        return this;
    }

    /** @see org.sagebionetworks.bridge.models.healthdata.HealthDataRecord#getUserSharingScope */
    public ParticipantOption.SharingScope getUserSharingScope() {
        return userSharingScope;
    }
    
    /** @see org.sagebionetworks.bridge.models.healthdata.HealthDataRecord#getUserSharingScope */
    public HealthDataRecordBuilder withUserSharingScope(ParticipantOption.SharingScope userSharingScope) {
        this.userSharingScope = userSharingScope;
        return this;
    }
    /** @see org.sagebionetworks.bridge.models.healthdata.HealthDataRecord#getUserExternalId */
    public String getUserExternalId() {
        return userExternalId;
    }
        
    /** @see org.sagebionetworks.bridge.models.healthdata.HealthDataRecord#getUserExternalId */
    public HealthDataRecordBuilder withUserExternalId(String externalId) {
        this.userExternalId = externalId;
        return this;
    }

    /** @see org.sagebionetworks.bridge.models.healthdata.HealthDataRecord#getVersion */
    public Long getVersion() {
        return version;
    }

    /** @see org.sagebionetworks.bridge.models.healthdata.HealthDataRecord#getVersion */
    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * <p>
     * Builds and validates the HealthDataRecord object. This throws an InvalidEntityException if validation fails. See
     * {@link org.sagebionetworks.bridge.validators.HealthDataRecordValidator} for validation preconditions.
     * </p>
     * <p>
     * This builder also adds reasonable defaults to fields that are null or unspecified. Specifically:
     *   <ul>
     *     <li>createdOn defaults to the current time</li>
     *     <li>data and metadata default to an empty ObjectNode</li>
     *     <li>uploadDate defaults to the current calendar date (as measured in Pacific local time)</li>
     *     <li>userSharingScope defaults to NO_SHARING</li>
     *   </ul>
     * </p>
     */
    public HealthDataRecord build() {
        // default values
        if (createdOn == null) {
            createdOn = DateUtils.getCurrentMillisFromEpoch();
        }
        if (data == null) {
            data = BridgeObjectMapper.get().createObjectNode();
        }
        if (metadata == null) {
            metadata = BridgeObjectMapper.get().createObjectNode();
        }
        if (uploadDate == null) {
            uploadDate = DateUtils.getCurrentCalendarDateInLocalTime();
        }
        if (userSharingScope == null) {
            userSharingScope = ParticipantOption.SharingScope.NO_SHARING;
        }

        // build and validate
        HealthDataRecord record = buildUnvalidated();
        Validate.entityThrowingException(HealthDataRecordValidator.INSTANCE, record);
        return record;
    }

    /** Builds a HealthDataRecord object. Subclasses should implement this method. */
    protected abstract HealthDataRecord buildUnvalidated();
}
