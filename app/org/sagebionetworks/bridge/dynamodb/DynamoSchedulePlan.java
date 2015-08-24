package org.sagebionetworks.bridge.dynamodb;

import java.util.Objects;

import org.sagebionetworks.bridge.BridgeConstants;
import org.sagebionetworks.bridge.exceptions.BridgeServiceException;
import org.sagebionetworks.bridge.json.BridgeObjectMapper;
import org.sagebionetworks.bridge.json.DateTimeJsonDeserializer;
import org.sagebionetworks.bridge.json.DateTimeJsonSerializer;
import org.sagebionetworks.bridge.json.JsonUtils;
import org.sagebionetworks.bridge.models.schedules.SchedulePlan;
import org.sagebionetworks.bridge.models.schedules.ScheduleStrategy;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMarshalling;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBVersionAttribute;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * This object represents the row in DynamoDB, but also converts the strategy JSON column in DynamoDB into a sub-class
 * of the ScheduleStrategy object, which implements specific algorithms for assigning users their schedules.
 */
@DynamoDBTable(tableName = "SchedulePlan")
public class DynamoSchedulePlan implements SchedulePlan {

    private static final String GUID_PROPERTY = "guid";
    private static final String LABEL_PROPERTY = "label";
    private static final String STUDY_KEY_PROPERTY = "studyKey";
    private static final String MODIFIED_ON_PROPERTY = "modifiedOn";
    private static final String STRATEGY_PROPERTY = "strategy";
    private static final String VERSION_PROPERTY = "version";
    
    private String guid;
    private String label;
    private String studyKey;
    private Long version;
    private long modifiedOn;
    private ScheduleStrategy strategy;

    public static DynamoSchedulePlan fromJson(JsonNode node) {
        DynamoSchedulePlan plan = new DynamoSchedulePlan();
        plan.setGuid(JsonUtils.asText(node, GUID_PROPERTY));
        plan.setLabel(JsonUtils.asText(node, LABEL_PROPERTY));
        plan.setModifiedOn(JsonUtils.asMillisSinceEpoch(node, MODIFIED_ON_PROPERTY));
        plan.setStudyKey(JsonUtils.asText(node, STUDY_KEY_PROPERTY));
        plan.setData(JsonUtils.asObjectNode(node, STRATEGY_PROPERTY));
        plan.setVersion(JsonUtils.asLong(node, VERSION_PROPERTY));
        return plan;
    }
    
    @Override
    @DynamoDBHashKey
    public String getStudyKey() {
        return studyKey;
    }
    @Override
    public void setStudyKey(String studyKey) {
        this.studyKey = studyKey;
    }
    @Override
    @DynamoDBRangeKey
    public String getGuid() {
        return guid;
    }
    @Override
    public void setGuid(String guid) {
        this.guid = guid;
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
    @DynamoDBVersionAttribute
    public Long getVersion() {
        return version;
    }
    @Override
    public void setVersion(Long version) {
        this.version = version;
    }
    @Override
    @DynamoDBIgnore
    public ScheduleStrategy getStrategy() {
        return strategy;
    }
    @Override
    public void setStrategy(ScheduleStrategy strategy) {
        this.strategy = strategy;
    }
    @Override
    @DynamoDBAttribute
    public String getLabel() {
        return label;
    }
    @Override
    public void setLabel(String label) {
        this.label = label;
    }
    @DynamoDBAttribute(attributeName="strategy")
    @DynamoDBMarshalling(marshallerClass = JsonNodeMarshaller.class)
    @JsonIgnore
    public ObjectNode getData() {
        ObjectNode node = BridgeObjectMapper.get().valueToTree(strategy);
        node.put("type", strategy.getClass().getSimpleName());
        return node;
    }
    public void setData(ObjectNode data) {
        if (data != null) {
            try {
                String typeName = JsonUtils.asText(data, "type");
                String className = BridgeConstants.SCHEDULE_STRATEGY_PACKAGE + typeName;
                Class<?> clazz = Class.forName(className);
                strategy = (ScheduleStrategy)BridgeObjectMapper.get().treeToValue(data, clazz);
            } catch (JsonProcessingException | ClassNotFoundException e) {
                throw new BridgeServiceException(e);
            }
        }
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Objects.hashCode(guid);
        result = prime * result + Objects.hashCode(label);
        result = prime * result + Objects.hashCode(modifiedOn);
        result = prime * result + Objects.hashCode(strategy);
        result = prime * result + Objects.hashCode(studyKey);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        DynamoSchedulePlan other = (DynamoSchedulePlan) obj;
        return (Objects.equals(guid, other.guid) && Objects.equals(label, other.label)
                && Objects.equals(modifiedOn, other.modifiedOn) && Objects.equals(strategy, other.strategy)
                && Objects.equals(label, other.label) && Objects.equals(studyKey, other.studyKey));
    }

    @Override
    public String toString() {
        return String.format("DynamoSchedulePlan [guid=%s, label=%s, studyKey=%s, modifiedOn=%s, strategy=%s]",
                guid, label, studyKey, modifiedOn, strategy);
    }

}
