package org.sagebionetworks.bridge.validators;

import org.sagebionetworks.bridge.models.healthdata.HealthDataRecord;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/** Validator for {@link org.sagebionetworks.bridge.models.healthdata.HealthDataRecord}. */
public class HealthDataRecordValidator implements Validator {
    /** Singleton instance of this validator. */
    public static HealthDataRecordValidator INSTANCE = new HealthDataRecordValidator();

    /** {@inheritDoc} */
    @Override
    public boolean supports(Class<?> clazz) {
        return HealthDataRecord.class.isAssignableFrom(clazz);
    }

    /**
     * <p>
     * Validates the given object as a valid HealthDataRecord instance. This will flag errors in the following
     * conditions:
     * <ul>
     * <li>createdOn is null</li>
     * <li>data is null (in Java or in JSON)</li>
     * <li>data is not a map (but empty maps are okay)</li>
     * <li>healthCode is null or empty</li>
     * <li>metadata is null (in Java or in JSON)</li>
     * <li>metadata is not a map (but empty maps are okay)</li>
     * <li>schemaId is null or empty</li>
     * <li>schemaRevision is zero or negative</li>
     * <li>studyId is null or empty</li>
     * <li>uploadDate is null</li>
     * <li>userSharingScope is null</li>
     * </ul>
     * </p>
     * <p>
     * This validator accpets null record ID, as it may not have been assigned yet. However, if it's an empty string,
     * it will flag an error.
     * </p>
     *
     * @see org.springframework.validation.Validator#validate
     */
    @Override
    public void validate(Object object, Errors errors) {
        if (object == null) {
            errors.rejectValue("HealthDataRecord", Validate.CANNOT_BE_NULL);
        } else if (!(object instanceof HealthDataRecord)) {
            errors.rejectValue("HealthDataRecord", Validate.WRONG_TYPE);
        } else {
            HealthDataRecord record = (HealthDataRecord) object;

            // createdOn
            if (record.getCreatedOn() == null) {
                errors.rejectValue("createdOn", Validate.CANNOT_BE_NULL);
            }

            // data
            JsonNode data = record.getData();
            if (data == null) {
                errors.rejectValue("data", Validate.CANNOT_BE_NULL);
            } else if (!data.isObject()) {
                // We don't need to check isNull(). If it's null, then it's not an object.
                errors.rejectValue("data", Validate.WRONG_TYPE);
            }

            // health code
            if (StringUtils.isBlank(record.getHealthCode())) {
                errors.rejectValue("healthCode", Validate.CANNOT_BE_BLANK);
            }

            // id
            String id = record.getId();
            if (id != null && StringUtils.isBlank(id)) {
                errors.rejectValue("id", Validate.CANNOT_BE_EMPTY_STRING);
            }

            // metadata
            JsonNode metadata = record.getMetadata();
            if (metadata == null) {
                errors.rejectValue("metadata", Validate.CANNOT_BE_NULL);
            } else if (!metadata.isObject()) {
                // We don't need to check isNull(). If it's null, then it's not an object.
                errors.rejectValue("metadata", Validate.WRONG_TYPE);
            }

            // schema ID is non-null and non-empty
            if (StringUtils.isBlank(record.getSchemaId())) {
                errors.rejectValue("schemaId", Validate.CANNOT_BE_BLANK);
            }

            // schema revision is positive
            if (record.getSchemaRevision() <= 0) {
                errors.rejectValue("schemaRevision", Validate.CANNOT_BE_ZERO_OR_NEGATIVE);
            }

            // schema ID is non-null and non-empty
            if (StringUtils.isBlank(record.getStudyId())) {
                errors.rejectValue("studyId", Validate.CANNOT_BE_BLANK);
            }

            // upload date is non-null
            if (record.getUploadDate() == null) {
                errors.rejectValue("uploadDate", Validate.CANNOT_BE_NULL);
            }

            // user sharing scope is non-null
            if (record.getUserSharingScope() == null) {
                errors.rejectValue("userSharingScope", Validate.CANNOT_BE_NULL);
            }
        }
    }
}
