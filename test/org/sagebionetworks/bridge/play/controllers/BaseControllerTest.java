package org.sagebionetworks.bridge.play.controllers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Test;

import play.mvc.Http;

import org.sagebionetworks.bridge.BridgeConstants;
import org.sagebionetworks.bridge.config.BridgeConfig;
import org.sagebionetworks.bridge.config.BridgeConfigFactory;
import org.sagebionetworks.bridge.exceptions.InvalidEntityException;
import org.sagebionetworks.bridge.json.BridgeObjectMapper;
import org.sagebionetworks.bridge.play.controllers.ApplicationController;
import org.sagebionetworks.bridge.play.controllers.BaseController;

/** Test class for basic utility functions in BaseController. */
@SuppressWarnings("unchecked")
public class BaseControllerTest {
    private static final String DUMMY_JSON = "{\"dummy-key\":\"dummy-value\"}";
    private static final String STUDY_IDENTIFIER = "studyName";
    private static final String HOSTNAME_POSTFIX = BridgeConfigFactory.getConfig().getStudyHostnamePostfix();
    private static final String HOSTNAME = STUDY_IDENTIFIER+HOSTNAME_POSTFIX;

    @Test
    public void testParseJsonFromText() {
        // mock request
        Http.RequestBody mockBody = mock(Http.RequestBody.class);
        when(mockBody.asText()).thenReturn(DUMMY_JSON);

        Http.Request mockRequest = mock(Http.Request.class);
        when(mockRequest.body()).thenReturn(mockBody);

        // execute and validate
        Map<String, String> resultMap = BaseController.parseJson(mockRequest, Map.class);
        assertEquals(1, resultMap.size());
        assertEquals("dummy-value", resultMap.get("dummy-key"));
    }

    @Test
    public void testParseJsonFromNode() throws Exception {
        // mock request
        Http.RequestBody mockBody = mock(Http.RequestBody.class);
        when(mockBody.asText()).thenReturn(null);
        when(mockBody.asJson()).thenReturn(BridgeObjectMapper.get().readTree(DUMMY_JSON));

        Http.Request mockRequest = mock(Http.Request.class);
        when(mockRequest.body()).thenReturn(mockBody);

        // execute and validate
        Map<String, String> resultMap = BaseController.parseJson(mockRequest, Map.class);
        assertEquals(1, resultMap.size());
        assertEquals("dummy-value", resultMap.get("dummy-key"));
    }

    @Test(expected = InvalidEntityException.class)
    public void testParseJsonError() {
        Http.Request mockRequest = mock(Http.Request.class);
        when(mockRequest.body()).thenThrow(RuntimeException.class);
        BaseController.parseJson(mockRequest, Map.class);
    }

    @Test(expected = InvalidEntityException.class)
    public void testParseJsonNoJson() throws Exception {
        // mock request
        Http.RequestBody mockBody = mock(Http.RequestBody.class);
        when(mockBody.asText()).thenReturn(null);
        when(mockBody.asJson()).thenReturn(null);

        Http.Request mockRequest = mock(Http.Request.class);
        when(mockRequest.body()).thenReturn(mockBody);

        // execute and validate
        BaseController.parseJson(mockRequest, Map.class);
    }
    
    @Test
    @SuppressWarnings("deprecation")
    public void studyExtractableFromBridgeHostHeader() {
        Http.Request mockRequest = mock(Http.Request.class);
        when(mockRequest.getHeader(BridgeConstants.BRIDGE_HOST_HEADER)).thenReturn(HOSTNAME);
        
        Http.Context mockContext = mock(Http.Context.class);
        when(mockContext.request()).thenReturn(mockRequest);
        
        Http.Context.current.set(mockContext);
        
        ApplicationController controller = new ApplicationController();
        BridgeConfig mockConfig = mock(BridgeConfig.class);
        when(mockConfig.getStudyHostnamePostfix()).thenReturn(HOSTNAME_POSTFIX);
        controller.setBridgeConfig(mockConfig);
        
        String retrievedIdentifier = controller.getStudyIdentifier();
        assertEquals(STUDY_IDENTIFIER, retrievedIdentifier);
    }
    
    @Test 
    @SuppressWarnings("deprecation")
    public void studyExtractableFromBridgeStudyHeader() {
        Http.Request mockRequest = mock(Http.Request.class);
        when(mockRequest.getHeader(BridgeConstants.BRIDGE_STUDY_HEADER)).thenReturn(STUDY_IDENTIFIER);

        Http.Context mockContext = mock(Http.Context.class);
        when(mockContext.request()).thenReturn(mockRequest);
        
        Http.Context.current.set(mockContext);
        
        ApplicationController controller = new ApplicationController();
        
        BridgeConfig mockConfig = mock(BridgeConfig.class);
        when(mockConfig.getStudyHostnamePostfix()).thenReturn(HOSTNAME_POSTFIX);
        controller.setBridgeConfig(mockConfig);
        
        String studyIdentifier = controller.getStudyIdentifier();
        assertEquals(STUDY_IDENTIFIER, studyIdentifier);
    }
    
    @Test
    @SuppressWarnings("deprecation")
    public void whenNoHeaderPresentUseHost() {
        Http.Request mockRequest = mock(Http.Request.class);
        
        when(mockRequest.host()).thenReturn(HOSTNAME);
        
        Http.Context mockContext = mock(Http.Context.class);
        when(mockContext.request()).thenReturn(mockRequest);

        Http.Context.current.set(mockContext);
        
        ApplicationController controller = new ApplicationController();
        BridgeConfig mockConfig = mock(BridgeConfig.class);
        when(mockConfig.getStudyHostnamePostfix()).thenReturn(HOSTNAME_POSTFIX);
        controller.setBridgeConfig(mockConfig);
        
        String retrievedIdentifier = controller.getStudyIdentifier();
        assertEquals(STUDY_IDENTIFIER, retrievedIdentifier);
    }
    
    @Test
    @SuppressWarnings("deprecation")
    public void studyHeaderHigherPrecedenceThanOtherStudyLocations() {
        Http.Request mockRequest = mock(Http.Request.class);
        
        when(mockRequest.getHeader(BridgeConstants.BRIDGE_STUDY_HEADER)).thenReturn(STUDY_IDENTIFIER);
        when(mockRequest.getHeader(BridgeConstants.BRIDGE_HOST_HEADER)).thenReturn("badStudyId"+HOSTNAME_POSTFIX);
        when(mockRequest.host()).thenReturn("badStudyId"+HOSTNAME_POSTFIX);
        
        Http.Context mockContext = mock(Http.Context.class);
        when(mockContext.request()).thenReturn(mockRequest);
        
        Http.Context.current.set(mockContext);
        
        ApplicationController controller = new ApplicationController();
        BridgeConfig mockConfig = mock(BridgeConfig.class);
        when(mockConfig.getStudyHostnamePostfix()).thenReturn(HOSTNAME_POSTFIX);
        controller.setBridgeConfig(mockConfig);
        
        String retrievedIdentifier = controller.getStudyIdentifier();
        assertEquals(STUDY_IDENTIFIER, retrievedIdentifier);
    }
    
    
}
