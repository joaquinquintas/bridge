package org.sagebionetworks.bridge.play.controllers;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.InOrder;
import org.sagebionetworks.bridge.cache.CacheProvider;
import org.sagebionetworks.bridge.dao.ParticipantOption.SharingScope;
import org.sagebionetworks.bridge.models.accounts.User;
import org.sagebionetworks.bridge.models.accounts.UserSession;
import org.sagebionetworks.bridge.models.studies.Study;
import org.sagebionetworks.bridge.models.studies.StudyIdentifier;
import org.sagebionetworks.bridge.play.controllers.ConsentController;
import org.sagebionetworks.bridge.services.ConsentService;
import org.sagebionetworks.bridge.services.ParticipantOptionsService;
import org.sagebionetworks.bridge.services.StudyService;

public class ConsentControllerMockedTest {

    @Test
    public void testChangeSharingScope() {

        UserSession session = mock(UserSession.class);
        StudyIdentifier studyId = mock(StudyIdentifier.class);
        when(session.getStudyIdentifier()).thenReturn(studyId);
        User user = mock(User.class);
        when(user.getHealthCode()).thenReturn("healthCode");
        when(session.getUser()).thenReturn(user);

        ConsentController controller = spy(new ConsentController());
        doReturn(session).when(controller).getAuthenticatedAndConsentedSession();

        StudyService studyService = mock(StudyService.class);
        Study study = mock(Study.class);
        when(studyService.getStudy(studyId)).thenReturn(study);
        controller.setStudyService(studyService);

        ConsentService consentService = mock(ConsentService.class);
        controller.setConsentService(consentService);

        ParticipantOptionsService optionsService = mock(ParticipantOptionsService.class);
        controller.setOptionsService(optionsService);

        controller.setCacheProvider(mock(CacheProvider.class));

        controller.changeSharingScope(SharingScope.NO_SHARING, "message");
        InOrder inOrder = inOrder(optionsService, consentService);
        inOrder.verify(optionsService).setOption(study, "healthCode", SharingScope.NO_SHARING);
        inOrder.verify(consentService).emailConsentAgreement(study, user);
    }
}
