package org.sagebionetworks.bridge.upload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import org.sagebionetworks.bridge.BridgeConstants;
import org.sagebionetworks.bridge.TestUtils;
import org.sagebionetworks.bridge.dynamodb.DynamoHealthDataDao;
import org.sagebionetworks.bridge.dynamodb.DynamoStudy;
import org.sagebionetworks.bridge.dynamodb.DynamoUpload2;
import org.sagebionetworks.bridge.dynamodb.DynamoUploadFieldDefinition;
import org.sagebionetworks.bridge.dynamodb.DynamoUploadSchema;
import org.sagebionetworks.bridge.json.BridgeObjectMapper;
import org.sagebionetworks.bridge.models.healthdata.HealthDataRecordBuilder;
import org.sagebionetworks.bridge.models.upload.UploadFieldDefinition;
import org.sagebionetworks.bridge.models.upload.UploadFieldType;
import org.sagebionetworks.bridge.models.upload.UploadSchemaType;
import org.sagebionetworks.bridge.services.UploadSchemaService;

public class IosSchemaValidationHandler2Test {
    private static final String TEST_HEALTHCODE = "test-healthcode";
    private static final String TEST_STUDY_ID = "test-study";
    private static final String TEST_UPLOAD_DATE_STRING = "2015-04-13";
    private static final String TEST_UPLOAD_ID = "test-upload";

    private UploadValidationContext context;
    private IosSchemaValidationHandler2 handler;

    @Before
    public void setup() {
        // set up common params for test context
        // dummy study, all we need is the ID
        DynamoStudy study = TestUtils.getValidStudy();
        study.setIdentifier(TEST_STUDY_ID);

        // For upload, we need uploadId, healthCode, and uploadDate
        DynamoUpload2 upload = new DynamoUpload2();
        upload.setUploadId(TEST_UPLOAD_ID);
        upload.setHealthCode(TEST_HEALTHCODE);
        upload.setUploadDate(LocalDate.parse(TEST_UPLOAD_DATE_STRING));

        context = new UploadValidationContext();
        context.setStudy(study);
        context.setUpload(upload);

        // set up test schemas
        DynamoUploadSchema surveySchema = new DynamoUploadSchema();
        surveySchema.setStudyId(TEST_STUDY_ID);
        surveySchema.setSchemaId("test-survey");
        surveySchema.setRevision(1);
        surveySchema.setName("iOS Survey");
        surveySchema.setSchemaType(UploadSchemaType.IOS_SURVEY);
        surveySchema.setFieldDefinitions(ImmutableList.<UploadFieldDefinition>of(
                new DynamoUploadFieldDefinition.Builder().withName("foo").withType(UploadFieldType.STRING).build(),
                new DynamoUploadFieldDefinition.Builder().withName("bar").withType(UploadFieldType.INT).build(),
                new DynamoUploadFieldDefinition.Builder().withName("bar_unit").withType(UploadFieldType.STRING)
                        .build(),
                new DynamoUploadFieldDefinition.Builder().withName("baz")
                        .withType(UploadFieldType.ATTACHMENT_JSON_BLOB).build(),
                new DynamoUploadFieldDefinition.Builder().withName("optional").withRequired(false)
                        .withType(UploadFieldType.STRING).build(),
                new DynamoUploadFieldDefinition.Builder().withName("optional_attachment").withRequired(false)
                        .withType(UploadFieldType.ATTACHMENT_JSON_BLOB).build()));

        DynamoUploadSchema jsonDataSchema = new DynamoUploadSchema();
        jsonDataSchema.setStudyId(TEST_STUDY_ID);
        jsonDataSchema.setSchemaId("json-data");
        jsonDataSchema.setRevision(1);
        jsonDataSchema.setName("JSON Data");
        jsonDataSchema.setSchemaType(UploadSchemaType.IOS_DATA);
        jsonDataSchema.setFieldDefinitions(ImmutableList.<UploadFieldDefinition>of(
                new DynamoUploadFieldDefinition.Builder().withName("string.json.string")
                        .withType(UploadFieldType.STRING).build(),
                new DynamoUploadFieldDefinition.Builder().withName("blob.json.blob")
                        .withType(UploadFieldType.ATTACHMENT_JSON_BLOB).build(),
                new DynamoUploadFieldDefinition.Builder().withName("optional").withRequired(false)
                        .withType(UploadFieldType.STRING).build(),
                new DynamoUploadFieldDefinition.Builder().withName("optional_attachment").withRequired(false)
                        .withType(UploadFieldType.ATTACHMENT_JSON_BLOB).build()));

        DynamoUploadSchema nonJsonDataSchema = new DynamoUploadSchema();
        nonJsonDataSchema.setStudyId(TEST_STUDY_ID);
        nonJsonDataSchema.setSchemaId("non-json-data");
        nonJsonDataSchema.setRevision(1);
        nonJsonDataSchema.setName("Non-JSON Data");
        nonJsonDataSchema.setSchemaType(UploadSchemaType.IOS_DATA);
        nonJsonDataSchema.setFieldDefinitions(ImmutableList.<UploadFieldDefinition>of(
                new DynamoUploadFieldDefinition.Builder().withName("nonJsonFile.txt")
                        .withType(UploadFieldType.ATTACHMENT_BLOB).build(),
                new DynamoUploadFieldDefinition.Builder().withName("jsonFile.json")
                        .withType(UploadFieldType.ATTACHMENT_JSON_BLOB).build(),
                new DynamoUploadFieldDefinition.Builder().withName("optional").withRequired(false)
                        .withType(UploadFieldType.STRING).build(),
                new DynamoUploadFieldDefinition.Builder().withName("optional_attachment").withRequired(false)
                        .withType(UploadFieldType.ATTACHMENT_JSON_BLOB).build()));

        DynamoUploadSchema mixedSchema = new DynamoUploadSchema();
        mixedSchema.setStudyId(TEST_STUDY_ID);
        mixedSchema.setSchemaId("mixed-data");
        mixedSchema.setRevision(1);
        mixedSchema.setName("Mixed Data");
        mixedSchema.setSchemaType(UploadSchemaType.IOS_DATA);
        mixedSchema.setFieldDefinitions(ImmutableList.<UploadFieldDefinition>of(
                new DynamoUploadFieldDefinition.Builder().withName("nonJsonFile.txt")
                        .withType(UploadFieldType.ATTACHMENT_BLOB).build(),
                new DynamoUploadFieldDefinition.Builder().withName("attachment.json")
                        .withType(UploadFieldType.ATTACHMENT_JSON_BLOB).build(),
                new DynamoUploadFieldDefinition.Builder().withName("inline.json")
                        .withType(UploadFieldType.INLINE_JSON_BLOB).build(),
                new DynamoUploadFieldDefinition.Builder().withName("field.json.attachment")
                        .withType(UploadFieldType.ATTACHMENT_JSON_BLOB).build(),
                new DynamoUploadFieldDefinition.Builder().withName("field.json.string")
                        .withType(UploadFieldType.STRING).build(),
                new DynamoUploadFieldDefinition.Builder().withName("optional").withRequired(false)
                        .withType(UploadFieldType.STRING).build(),
                new DynamoUploadFieldDefinition.Builder().withName("optional_attachment").withRequired(false)
                        .withType(UploadFieldType.ATTACHMENT_JSON_BLOB).build()));

        // mock upload schema service
        UploadSchemaService mockSchemaService = mock(UploadSchemaService.class);
        when(mockSchemaService.getUploadSchema(study, "test-survey")).thenReturn(surveySchema);
        when(mockSchemaService.getUploadSchema(study, "json-data")).thenReturn(jsonDataSchema);
        when(mockSchemaService.getUploadSchema(study, "non-json-data")).thenReturn(nonJsonDataSchema);
        when(mockSchemaService.getUploadSchema(study, "mixed-data")).thenReturn(mixedSchema);

        // set up handler
        handler = new IosSchemaValidationHandler2();
        handler.setUploadSchemaService(mockSchemaService);

        // health data dao is only used for getBuilder(), so we can just create one without any depedencies
        handler.setHealthDataDao(new DynamoHealthDataDao());
    }

    @Test
    public void survey() throws Exception {
        // fill in context with survey data
        String infoJsonText = "{\n" +
                "   \"files\":[{\n" +
                "       \"filename\":\"foo.json\",\n" +
                "       \"timestamp\":\"2015-04-02T03:26:59-07:00\"\n" +
                "   },{\n" +
                "       \"filename\":\"bar.json\",\n" +
                "       \"timestamp\":\"2015-04-02T03:27:09-07:00\"\n" +
                "   },{\n" +
                "       \"filename\":\"baz.json\",\n" +
                "       \"timestamp\":\"2015-04-02T03:24:01-07:00\"\n" +
                "   }],\n" +
                "   \"item\":\"test-survey\"\n" +
                "}";
        JsonNode infoJsonNode = BridgeObjectMapper.get().readTree(infoJsonText);

        String fooAnswerJsonText = "{\n" +
                "   \"questionType\":0,\n" +
                "   \"textAnswer\":\"foo answer\",\n" +
                "   \"startDate\":\"2015-04-02T03:26:57-07:00\",\n" +
                "   \"questionTypeName\":\"Text\",\n" +
                "   \"item\":\"foo\",\n" +
                "   \"endDate\":\"2015-04-02T03:26:59-07:00\"\n" +
                "}";
        JsonNode fooAnswerJsonNode = BridgeObjectMapper.get().readTree(fooAnswerJsonText);

        String barAnswerJsonText = "{\n" +
                "   \"questionType\":0,\n" +
                "   \"numericAnswer\":42,\n" +
                "   \"unit\":\"lb\",\n" +
                "   \"startDate\":\"2015-04-02T03:27:05-07:00\",\n" +
                "   \"questionTypeName\":\"Integer\",\n" +
                "   \"item\":\"bar\",\n" +
                "   \"endDate\":\"2015-04-02T03:27:09-07:00\"\n" +
                "}";
        JsonNode barAnswerJsonNode = BridgeObjectMapper.get().readTree(barAnswerJsonText);

        String bazAnswerJsonText = "{\n" +
                "   \"questionType\":0,\n" +
                "   \"choiceAnswers\":[\"survey\", \"blob\"],\n" +
                "   \"startDate\":\"2015-04-02T03:23:59-07:00\",\n" +
                "   \"questionTypeName\":\"MultipleChoice\",\n" +
                "   \"item\":\"baz\",\n" +
                "   \"endDate\":\"2015-04-02T03:24:01-07:00\"\n" +
                "}";
        JsonNode bazAnswerJsonNode = BridgeObjectMapper.get().readTree(bazAnswerJsonText);

        context.setJsonDataMap(ImmutableMap.of(
                "info.json", infoJsonNode,
                "foo.json", fooAnswerJsonNode,
                "bar.json", barAnswerJsonNode,
                "baz.json", bazAnswerJsonNode));
        context.setUnzippedDataMap(ImmutableMap.<String, byte[]>of());

        // execute
        handler.handle(context);

        // validate
        HealthDataRecordBuilder recordBuilder = context.getHealthDataRecordBuilder();
        validateCommonRecordProps(recordBuilder);
        assertEquals(DateTime.parse("2015-04-02T03:27:09-07:00").getMillis(),
                recordBuilder.getCreatedOn().longValue());
        assertEquals("test-survey", recordBuilder.getSchemaId());
        assertEquals(1, recordBuilder.getSchemaRevision());

        JsonNode dataNode = recordBuilder.getData();
        assertEquals(3, dataNode.size());
        assertEquals("foo answer", dataNode.get("foo").textValue());
        assertEquals(42, dataNode.get("bar").intValue());
        assertEquals("lb", dataNode.get("bar_unit").textValue());

        Map<String, byte[]> attachmentMap = context.getAttachmentsByFieldName();
        assertEquals(1, attachmentMap.size());
        JsonNode blobNode = BridgeObjectMapper.get().readTree(attachmentMap.get("baz"));
        assertEquals(2, blobNode.size());
        assertEquals("survey", blobNode.get(0).textValue());
        assertEquals("blob", blobNode.get(1).textValue());

        // We should have no messages.
        assertTrue(context.getMessageList().isEmpty());
    }

    @Test
    public void jsonData() throws Exception {
        // fill in context with JSON data
        String infoJsonText = "{\n" +
                "   \"files\":[{\n" +
                "       \"filename\":\"string.json\",\n" +
                "       \"timestamp\":\"2015-04-13T18:48:02-07:00\"\n" +
                "   },{\n" +
                "       \"filename\":\"blob.json\",\n" +
                "       \"timestamp\":\"2015-04-13T18:47:20-07:00\"\n" +
                "   }],\n" +
                "   \"item\":\"json-data\"\n" +
                "}";
        JsonNode infoJsonNode = BridgeObjectMapper.get().readTree(infoJsonText);

        String stringJsonText = "{\n" +
                "   \"string\":\"This is a string\"\n" +
                "}";
        JsonNode stringJsonNode = BridgeObjectMapper.get().readTree(stringJsonText);

        String blobJsonText = "{\n" +
                "   \"blob\":[\"This\", \"is\", \"a\", \"blob\"]\n" +
                "}";
        JsonNode blobJsonNode = BridgeObjectMapper.get().readTree(blobJsonText);

        context.setJsonDataMap(ImmutableMap.of(
                "info.json", infoJsonNode,
                "string.json", stringJsonNode,
                "blob.json", blobJsonNode));
        context.setUnzippedDataMap(ImmutableMap.<String, byte[]>of());

        // execute
        handler.handle(context);

        // validate
        HealthDataRecordBuilder recordBuilder = context.getHealthDataRecordBuilder();
        validateCommonRecordProps(recordBuilder);
        assertEquals(DateTime.parse("2015-04-13T18:48:02-07:00").getMillis(),
                recordBuilder.getCreatedOn().longValue());
        assertEquals("json-data", recordBuilder.getSchemaId());
        assertEquals(1, recordBuilder.getSchemaRevision());

        JsonNode dataNode = recordBuilder.getData();
        assertEquals(1, dataNode.size());
        assertEquals("This is a string", dataNode.get("string.json.string").textValue());

        Map<String, byte[]> attachmentMap = context.getAttachmentsByFieldName();
        assertEquals(1, attachmentMap.size());
        JsonNode blobNode = BridgeObjectMapper.get().readTree(attachmentMap.get("blob.json.blob"));
        assertEquals(4, blobNode.size());
        assertEquals("This", blobNode.get(0).textValue());
        assertEquals("is", blobNode.get(1).textValue());
        assertEquals("a", blobNode.get(2).textValue());
        assertEquals("blob", blobNode.get(3).textValue());

        // We should have no messages.
        assertTrue(context.getMessageList().isEmpty());
    }

    @Test
    public void nonJsonData() throws Exception {
        // fill in context
        String infoJsonText = "{\n" +
                "   \"files\":[{\n" +
                "       \"filename\":\"jsonFile.json\",\n" +
                "       \"timestamp\":\"2015-04-13T18:58:15-07:00\"\n" +
                "   },{\n" +
                "       \"filename\":\"nonJsonFile.txt\",\n" +
                "       \"timestamp\":\"2015-04-13T18:58:21-07:00\"\n" +
                "   }],\n" +
                "   \"item\":\"non-json-data\"\n" +
                "}";
        JsonNode infoJsonNode = BridgeObjectMapper.get().readTree(infoJsonText);

        String jsonJsonText = "{\n" +
                "   \"field\":\"This is JSON data\"\n" +
                "}";
        JsonNode jsonJsonNode = BridgeObjectMapper.get().readTree(jsonJsonText);

        context.setJsonDataMap(ImmutableMap.of(
                "info.json", infoJsonNode,
                "jsonFile.json", jsonJsonNode));
        context.setUnzippedDataMap(ImmutableMap.of("nonJsonFile.txt",
                "This is non-JSON data".getBytes(Charsets.UTF_8)));

        // execute
        handler.handle(context);

        // validate
        HealthDataRecordBuilder recordBuilder = context.getHealthDataRecordBuilder();
        validateCommonRecordProps(recordBuilder);
        assertEquals(DateTime.parse("2015-04-13T18:58:21-07:00").getMillis(),
                recordBuilder.getCreatedOn().longValue());
        assertEquals("non-json-data", recordBuilder.getSchemaId());
        assertEquals(1, recordBuilder.getSchemaRevision());

        JsonNode dataNode = recordBuilder.getData();
        assertEquals(0, dataNode.size());

        Map<String, byte[]> attachmentMap = context.getAttachmentsByFieldName();
        assertEquals(2, attachmentMap.size());

        JsonNode jsonJsonAttachmentNode = BridgeObjectMapper.get().readTree(attachmentMap.get("jsonFile.json"));
        assertEquals(1, jsonJsonAttachmentNode.size());
        assertEquals("This is JSON data", jsonJsonAttachmentNode.get("field").textValue());

        assertEquals("This is non-JSON data", new String(attachmentMap.get("nonJsonFile.txt"), Charsets.UTF_8));

        // We should have no messages.
        assertTrue(context.getMessageList().isEmpty());
    }

    @Test
    public void mixedData() throws Exception {
        // fill in context
        String infoJsonText = "{\n" +
                "   \"files\":[{\n" +
                "       \"filename\":\"nonJsonFile.txt\",\n" +
                "       \"timestamp\":\"2015-04-22T18:37:11-07:00\"\n" +
                "   },{\n" +
                "       \"filename\":\"attachment.json\",\n" +
                "       \"timestamp\":\"2015-04-22T18:38:22-07:00\"\n" +
                "   },{\n" +
                "       \"filename\":\"inline.json\",\n" +
                "       \"timestamp\":\"2015-04-22T18:39:33-07:00\"\n" +
                "   },{\n" +
                "       \"filename\":\"field.json\",\n" +
                "       \"timestamp\":\"2015-04-22T18:39:44-07:00\"\n" +
                "   }],\n" +
                "   \"item\":\"mixed-data\"\n" +
                "}";
        JsonNode infoJsonNode = BridgeObjectMapper.get().readTree(infoJsonText);

        String attachmentJsonText = "{\n" +
                "   \"attachment\":\"This is an attachment\"\n" +
                "}";
        JsonNode attachmentJsonNode = BridgeObjectMapper.get().readTree(attachmentJsonText);

        String inlineJsonText = "{\n" +
                "   \"string\":\"inline value\"\n" +
                "}";
        JsonNode inlineJsonNode = BridgeObjectMapper.get().readTree(inlineJsonText);

        String fieldJsonText = "{\n" +
                "   \"attachment\":[\"mixed\", \"data\", \"attachment\"],\n" +
                "   \"string\":\"This is a string\"\n" +
                "}";
        JsonNode fieldJsonNode = BridgeObjectMapper.get().readTree(fieldJsonText);

        context.setJsonDataMap(ImmutableMap.of(
                "info.json", infoJsonNode,
                "attachment.json", attachmentJsonNode,
                "inline.json", inlineJsonNode,
                "field.json", fieldJsonNode));
        context.setUnzippedDataMap(ImmutableMap.of("nonJsonFile.txt",
                "Non-JSON in mixed data".getBytes(Charsets.UTF_8)));

        // execute
        handler.handle(context);

        // validate
        HealthDataRecordBuilder recordBuilder = context.getHealthDataRecordBuilder();
        validateCommonRecordProps(recordBuilder);
        assertEquals(DateTime.parse("2015-04-22T18:39:44-07:00").getMillis(),
                recordBuilder.getCreatedOn().longValue());
        assertEquals("mixed-data", recordBuilder.getSchemaId());
        assertEquals(1, recordBuilder.getSchemaRevision());

        JsonNode dataNode = recordBuilder.getData();
        assertEquals(2, dataNode.size());
        assertEquals("This is a string", dataNode.get("field.json.string").textValue());

        JsonNode outputInlineJsonNode = dataNode.get("inline.json");
        assertEquals(1, outputInlineJsonNode.size());
        assertEquals("inline value", outputInlineJsonNode.get("string").textValue());

        Map<String, byte[]> attachmentMap = context.getAttachmentsByFieldName();
        assertEquals(3, attachmentMap.size());
        assertEquals("Non-JSON in mixed data", new String(attachmentMap.get("nonJsonFile.txt"), Charsets.UTF_8));

        JsonNode outputAttachmentJsonNode = BridgeObjectMapper.get().readTree(attachmentMap.get("attachment.json"));
        assertEquals(1, outputAttachmentJsonNode.size());
        assertEquals("This is an attachment", outputAttachmentJsonNode.get("attachment").textValue());

        JsonNode fieldJsonAttachmentNode = BridgeObjectMapper.get().readTree(attachmentMap.get(
                "field.json.attachment"));
        assertEquals(3, fieldJsonAttachmentNode.size());
        assertEquals("mixed", fieldJsonAttachmentNode.get(0).textValue());
        assertEquals("data", fieldJsonAttachmentNode.get(1).textValue());
        assertEquals("attachment", fieldJsonAttachmentNode.get(2).textValue());

        // We should have no messages.
        assertTrue(context.getMessageList().isEmpty());
    }

    private static void validateCommonRecordProps(HealthDataRecordBuilder recordBuilder) {
        LocalDate todaysDate = LocalDate.now(BridgeConstants.LOCAL_TIME_ZONE);

        assertEquals(TEST_HEALTHCODE, recordBuilder.getHealthCode());
        assertEquals(TEST_STUDY_ID, recordBuilder.getStudyId());
        assertEquals(todaysDate, recordBuilder.getUploadDate());
        assertEquals(TEST_UPLOAD_ID, recordBuilder.getUploadId());

        // Don't parse into the metadata. Just check that it exists and is an object node.
        assertTrue(recordBuilder.getMetadata().isObject());
    }
}
