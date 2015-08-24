package org.sagebionetworks.bridge.models.upload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.validation.MapBindingResult;

import org.sagebionetworks.bridge.dynamodb.DynamoUploadFieldDefinition;
import org.sagebionetworks.bridge.exceptions.InvalidEntityException;
import org.sagebionetworks.bridge.json.BridgeObjectMapper;
import org.sagebionetworks.bridge.json.JsonUtils;
import org.sagebionetworks.bridge.validators.UploadFieldDefinitionValidator;

public class UploadFieldDefinitionTest {
    @Test
    public void testBuilder() {
        UploadFieldDefinition fieldDef = new DynamoUploadFieldDefinition.Builder().withName("test-field")
                .withType(UploadFieldType.ATTACHMENT_BLOB).build();
        assertEquals("test-field", fieldDef.getName());
        assertTrue(fieldDef.isRequired());
        assertEquals(UploadFieldType.ATTACHMENT_BLOB, fieldDef.getType());
    }

    @Test(expected = InvalidEntityException.class)
    public void testNullName() {
        new DynamoUploadFieldDefinition.Builder().withType(UploadFieldType.ATTACHMENT_BLOB).build();
    }

    @Test(expected = InvalidEntityException.class)
    public void testEmptyName() {
        new DynamoUploadFieldDefinition.Builder().withName("").withType(UploadFieldType.ATTACHMENT_BLOB).build();
    }

    @Test(expected = InvalidEntityException.class)
    public void testNullType() {
        new DynamoUploadFieldDefinition.Builder().withName("test-field").build();
    }

    @Test
    public void testRequiredTrue() {
        UploadFieldDefinition fieldDef = new DynamoUploadFieldDefinition.Builder().withName("test-field")
                .withRequired(true).withType(UploadFieldType.ATTACHMENT_BLOB).build();
        assertEquals("test-field", fieldDef.getName());
        assertTrue(fieldDef.isRequired());
        assertEquals(UploadFieldType.ATTACHMENT_BLOB, fieldDef.getType());
    }

    @Test
    public void testRequiredFalse() {
        UploadFieldDefinition fieldDef = new DynamoUploadFieldDefinition.Builder().withName("test-field")
                .withRequired(false).withType(UploadFieldType.ATTACHMENT_BLOB).build();
        assertEquals("test-field", fieldDef.getName());
        assertFalse(fieldDef.isRequired());
        assertEquals(UploadFieldType.ATTACHMENT_BLOB, fieldDef.getType());
    }

    // branch coverage
    @Test
    public void validatorSupportsClass() {
        assertTrue(UploadFieldDefinitionValidator.INSTANCE.supports(UploadFieldDefinition.class));
    }

    // branch coverage
    @Test
    public void validatorSupportsSubclass() {
        assertTrue(UploadFieldDefinitionValidator.INSTANCE.supports(DynamoUploadFieldDefinition.class));
    }

    // branch coverage
    @Test
    public void validatorDoesntSupport() {
        assertFalse(UploadFieldDefinitionValidator.INSTANCE.supports(String.class));
    }

    // branch coverage
    // we call the validator directly, since Validate.validateThrowingException filters out nulls and wrong types
    @Test
    public void validateNull() {
        MapBindingResult errors = new MapBindingResult(new HashMap<>(), "UploadFieldDefinition");
        UploadFieldDefinitionValidator.INSTANCE.validate(null, errors);
        assertTrue(errors.hasErrors());
    }

    // branch coverage
    // we call the validator directly, since Validate.validateThrowingException filters out nulls and wrong types
    @Test
    public void validateWrongClass() {
        MapBindingResult errors = new MapBindingResult(new HashMap<>(), "UploadFieldDefinition");
        UploadFieldDefinitionValidator.INSTANCE.validate("this is the wrong class", errors);
        assertTrue(errors.hasErrors());
    }

    @Test
    public void testSerialization() throws Exception {
        // start with JSON
        String jsonText = "{\n" +
                "   \"name\":\"test-field\",\n" +
                "   \"required\":false,\n" +
                "   \"type\":\"INT\"\n" +
                "}";

        // convert to POJO
        UploadFieldDefinition fieldDef = BridgeObjectMapper.get().readValue(jsonText, UploadFieldDefinition.class);
        assertEquals("test-field", fieldDef.getName());
        assertFalse(fieldDef.isRequired());
        assertEquals(UploadFieldType.INT, fieldDef.getType());

        // convert back to JSON
        String convertedJson = BridgeObjectMapper.get().writeValueAsString(fieldDef);

        // then convert to a map so we can validate the raw JSON
        Map<String, Object> jsonMap = BridgeObjectMapper.get().readValue(convertedJson, JsonUtils.TYPE_REF_RAW_MAP);
        assertEquals(3, jsonMap.size());
        assertEquals("test-field", jsonMap.get("name"));
        assertFalse((boolean) jsonMap.get("required"));
        assertEquals("int", jsonMap.get("type"));
    }
}
