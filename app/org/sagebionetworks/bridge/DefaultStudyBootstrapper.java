package org.sagebionetworks.bridge;

import javax.annotation.PostConstruct;

import org.sagebionetworks.bridge.dynamodb.DynamoStudy;
import org.sagebionetworks.bridge.exceptions.EntityNotFoundException;
import org.sagebionetworks.bridge.models.studies.PasswordPolicy;
import org.sagebionetworks.bridge.models.studies.Study;
import org.sagebionetworks.bridge.services.StudyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("defaultStudyBootstrapper")
public class DefaultStudyBootstrapper {

    private StudyService studyService;

    @Autowired
    public final void setStudyService(StudyService studyService) {
        this.studyService = studyService;
    }
    
    @PostConstruct
    public void setupDefaultStudy() {
        try {
            studyService.getStudy("api");
        } catch(EntityNotFoundException e) {
        	System.out.println("PROBLEM!");
            Study study = new DynamoStudy();
            study.setName("Test Study");
            study.setIdentifier("api");
            study.setSponsorName("Wikilife");
            study.setMinAgeOfConsent(18);
            study.setConsentNotificationEmail("consent-bridge@wikilife.org");
            study.setTechnicalEmail("support@wikilife.org");
            study.setSupportEmail("support@sagebridge.org");
            // This is stormpath api (dev) directory.
            study.setStormpathHref("https://api.stormpath.com/v1/directories/1hk83x5snZzOEh13ssCnL0");
            System.out.println("PROBLEM!1");
            study.getUserProfileAttributes().add("phone");
            study.getUserProfileAttributes().add("can_be_recontacted");
            study.setPasswordPolicy(new PasswordPolicy(2, false, false, false, false));
            studyService.createStudy(study);
            System.out.println("PROBLEM!2");
        }
    }

}
