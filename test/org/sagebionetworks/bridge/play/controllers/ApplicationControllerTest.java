package org.sagebionetworks.bridge.play.controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.sagebionetworks.bridge.TestConstants.TEST_BASE_URL;
import static org.sagebionetworks.bridge.TestConstants.TIMEOUT;
import static play.mvc.Http.HeaderNames.ACCESS_CONTROL_ALLOW_HEADERS;
import static play.mvc.Http.HeaderNames.ACCESS_CONTROL_ALLOW_METHODS;
import static play.mvc.Http.HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN;
import static play.mvc.Http.HeaderNames.ACCESS_CONTROL_REQUEST_HEADERS;
import static play.mvc.Http.HeaderNames.ACCESS_CONTROL_REQUEST_METHOD;
import static play.mvc.Http.HeaderNames.ORIGIN;
import static play.mvc.Http.HeaderNames.REFERER;
import static play.mvc.Http.HeaderNames.X_FORWARDED_PROTO;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.bridge.BridgeConstants;
import org.sagebionetworks.bridge.config.BridgeConfigFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import play.libs.ws.WS;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

@ContextConfiguration("classpath:test-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class ApplicationControllerTest {

    @Test
    public void testPreflight() {
        running(testServer(3333), new Runnable() {
            @Override
            public void run() {
                WSRequest request = WS.url(TEST_BASE_URL + "/anything")
                        .setHeader(BridgeConstants.BRIDGE_HOST_HEADER, "api" + BridgeConfigFactory.getConfig().getStudyHostnamePostfix())
                        .setHeader(ACCESS_CONTROL_REQUEST_HEADERS, "accept, content-type")
                        .setHeader(ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .setHeader(ORIGIN, "https://some.remote.server.org");
                WSResponse response = request.options().get(TIMEOUT);
                assertEquals(200, response.getStatus());
                assertEquals("Should echo back the origin",
                        "https://some.remote.server.org", response.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN));
                assertEquals("Should echo back the access-control-allow-methods",
                        "POST", response.getHeader(ACCESS_CONTROL_ALLOW_METHODS));
                assertTrue("Should echo back the access-control-allow-request-headers",
                        response.getHeader(ACCESS_CONTROL_ALLOW_HEADERS).toLowerCase().contains("accept"));
                assertTrue("Should echo back the access-control-allow-request-headers",
                        response.getHeader(ACCESS_CONTROL_ALLOW_HEADERS).toLowerCase().contains("content-type"));
            }
        });
    }

    @Test
    public void testCors() {
        running(testServer(3333), new Runnable() {
            @Override
            public void run() {
                WSRequest request = WS.url(TEST_BASE_URL + "/")
                        .setHeader(BridgeConstants.BRIDGE_HOST_HEADER, "api" + BridgeConfigFactory.getConfig().getStudyHostnamePostfix())
                        .setHeader(ORIGIN, "https://some.remote.server.org")
                        .setHeader(REFERER, "https://some.remote.server.org");
                WSResponse response = request.get().get(TIMEOUT);
                assertEquals(200, response.getStatus());
            }
        });
    }

    @Test
    public void testHttpRedirect() {
        running(testServer(3333), new Runnable() {
            @Override
            public void run() {
                WSRequest request = WS.url(TEST_BASE_URL + "/")
                        .setFollowRedirects(Boolean.FALSE)
                        .setHeader(BridgeConstants.BRIDGE_HOST_HEADER, "api" + BridgeConfigFactory.getConfig().getStudyHostnamePostfix())
                        .setHeader(X_FORWARDED_PROTO, "http");
                WSResponse response = request.get().get(TIMEOUT);
                assertEquals(301, response.getStatus());
                assertNotNull(response.getHeader("location"));
                assertTrue(response.getHeader("location").startsWith("https://"));
            }
        });
    }
}
