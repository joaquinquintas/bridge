package org.sagebionetworks.bridge.models.tasks;

import org.sagebionetworks.bridge.models.BridgeEntity;

public interface TaskEvent extends BridgeEntity {

    public String getHealthCode();
    public void setHealthCode(String healthCode);
    
    public String getEventId();
    public void setEventId(String eventId);

    public String getAnswerValue();
    public void setAnswerValue(String answerValue);
    
    public Long getTimestamp();
    public void setTimestamp(Long timestamp);
    
}
