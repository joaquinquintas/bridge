package org.sagebionetworks.bridge.models.studies;

import java.util.Set;

import org.sagebionetworks.bridge.dynamodb.DynamoStudy;
import org.sagebionetworks.bridge.json.BridgeObjectMapper;
import org.sagebionetworks.bridge.models.BridgeEntity;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

/**
 * A Bridge study.
 *
 */
// This is required or Jackson searches for, and eventually, finds the same annotation for StudyIdentifer, 
// and attempts to use that to deserialize study (not what you want).
@JsonDeserialize(as=DynamoStudy.class)
public interface Study extends BridgeEntity, StudyIdentifier {

    public static final ObjectWriter STUDY_WRITER = new BridgeObjectMapper().writer(
        new SimpleFilterProvider().addFilter("filter", 
        SimpleBeanPropertyFilter.serializeAllExcept("stormpathHref")));

    public static final ObjectWriter STUDY_LIST_WRITER = new BridgeObjectMapper().writer(
        new SimpleFilterProvider().addFilter("filter",
        SimpleBeanPropertyFilter.filterOutAllExcept("name", "identifier")));
    
    /**
     * The display name of the study (will be seen by participants in email). This name makes the 
     * most sense when it starts with "The".
     * @return
     */
    public String getName();
    public void setName(String name);

    /**
     * The name of the institution or research group conducting the study. 
     * @return
     */
    public String getSponsorName();
    public void setSponsorName(String sponsorName);
    
    /**
     * A string that uniquely identifies the study, and serves as a domain within which accounts are 
     * scoped for that study. By convention, should be an instution acronym or tag, a dash, and then 
     * an acronym or short phrase for the study. For example "uw-asthma".
     */
    public String getIdentifier();
    public void setIdentifier(String identifier);
    
    /**
     * A strongly typed version of the study identifier.
     * @return
     */
    public StudyIdentifier getStudyIdentifier();
    
    /**
     * DynamoDB version number for optimistic locking of record.
     * @return
     */
    public Long getVersion();
    public void setVersion(Long version);
    
    /**
     * User must confirm that they are at least this many years old in order to 
     * participate in the study. 
     * @return
     */
    public int getMinAgeOfConsent();
    public void setMinAgeOfConsent(int minAge);
    
    /**
     * A study can be capped at a maximum number of participants. Past this number, attempts to 
     * sign up for the study will fail. 
     * @return
     */
    public int getMaxNumOfParticipants();
    public void setMaxNumOfParticipants(int maxParticipants);

    /**
     * The email address that will be given to study participants and other end user for all support 
     * requests and queries (technical, study-related, etc.). This can be a comma-separated list of 
     * email addresses.
     * @return
     */
    public String getSupportEmail();
    public void setSupportEmail(String email);
    
    /**
     * The email address for a technical contact who can coordinate with the Bridge Server team on 
     * issues related either to client development or hand-offs of the study data through the 
     * Bridge server. This can be a comma-separated list of email addresses.
     */
    public String getTechnicalEmail();
    public void setTechnicalEmail(String email);
    
    /**
     * Copies of all consent agreements, as well as rosters of all participants in a study, or any 
     * other study governance issues, will be emailed to this address. This can be a comma-separated 
     * list of email addresses. 
     * @return
     */
    public String getConsentNotificationEmail();
    public void setConsentNotificationEmail(String email);
    
    /**
     * The URI that identifies the Stormpath directory where all accounts for this study, in a given 
     * environment, will be stored.
     * @return
     */
    public String getStormpathHref();
    public void setStormpathHref(String stormpathHref);
    
    /**
     * Extension attributes that can be accepted on the UserProfile object for this study. These 
     * attributes will be exported with the participant roster. 
     * @return
     */
    public Set<String> getUserProfileAttributes();
    public void setUserProfileAttributes(Set<String> attributes);

    /**
     * The password policy for users signing up for this study. 
     * @return
     */
    public PasswordPolicy getPasswordPolicy();
    public void setPasswordPolicy(PasswordPolicy passwordPolicy);

    /**
     * The template for emails delivered to users during sign up, asking them to verify their email 
     * address. This template must at least include the "${url}" template variable, which will be 
     * used to place a link back to a page that completes the email verification for Bridge. 
     * @return
     */
    public EmailTemplate getVerifyEmailTemplate();
    public void setVerifyEmailTemplate(EmailTemplate template);
    
    /**
     * The template for emails delivered to users who ask to reset their passwords. This template 
     * must at least include the "${url}" template variable, which will be used to place a link 
     * back to a page that completes the password reset request. 
     * @return
     */
    public EmailTemplate getResetPasswordTemplate();
    public void setResetPasswordTemplate(EmailTemplate template);
    
    /**
     * Is this study active? Currently not in use, a de-activated study will be hidden from the 
     * study APIs and will no longer be available for use (a logical delete).
     * @return
     */
    public boolean isActive();
    public void setActive(boolean active);
}
