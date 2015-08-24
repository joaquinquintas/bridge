package org.sagebionetworks.bridge.models.surveys;

import java.util.List;

import org.sagebionetworks.bridge.json.BridgeTypeName;
import org.sagebionetworks.bridge.models.BridgeEntity;

@BridgeTypeName("SurveyResponse")
public interface SurveyResponse extends BridgeEntity {
    
    public enum Status {
        UNSTARTED,
        IN_PROGRESS,
        FINISHED;
    }

    public Long getVersion();
    public void setVersion(Long version);
    
    public String getIdentifier();
    public void setIdentifier(String identifier);
    
    public String getHealthCode();
    public void setHealthCode(String healthCode);
    
    public Status getStatus();
    
    public Long getStartedOn();
    public void setStartedOn(Long startedOn);
    
    public Long getCompletedOn();
    public void setCompletedOn(Long completedOn);

    public String getSurveyGuid();
    public long getSurveyCreatedOn();
    
    public List<SurveyAnswer> getAnswers();
    public void setAnswers(List<SurveyAnswer> answers);
 
}
