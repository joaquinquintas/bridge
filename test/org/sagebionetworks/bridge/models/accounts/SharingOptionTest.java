package org.sagebionetworks.bridge.models.accounts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.sagebionetworks.bridge.dao.ParticipantOption.SharingScope;
import org.sagebionetworks.bridge.exceptions.InvalidEntityException;
import org.sagebionetworks.bridge.models.accounts.SharingOption;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SharingOptionTest {

    @Test
    public void sharingOptionUsesCorrectDefaults() {

        ObjectNode node = JsonNodeFactory.instance.objectNode();
        // These JSON strings are the expected API to the client
        // A test failure wound indicate broken APIs
        node.put("scope", "sponsors_and_partners");

        SharingOption option = SharingOption.fromJson(node, 1);
        assertEquals(SharingScope.NO_SHARING, option.getSharingScope());

        option = SharingOption.fromJson(node, 2);
        assertEquals(SharingScope.SPONSORS_AND_PARTNERS, option.getSharingScope());

        try {
            node = JsonNodeFactory.instance.objectNode();
            option = SharingOption.fromJson(node, 2);
            fail("Should have thrown an invalid entity exception");
        } catch(InvalidEntityException e) {
            assertTrue(e.getMessage().contains("scope is required"));
        }

        node = JsonNodeFactory.instance.objectNode();
        // The following JSON strings are the expected API to the client
        // A test failure wound indicate broken APIs
        node.put("scope", "all_qualified_researchers");
        option = SharingOption.fromJson(node, 2);
        assertEquals(SharingScope.ALL_QUALIFIED_RESEARCHERS, option.getSharingScope());
    }

    @Test
    public void sharingOptionFailsGracefully() {
        SharingOption option = SharingOption.fromJson(null, 1);
        assertEquals(SharingScope.NO_SHARING, option.getSharingScope());
        
        try {
            option = SharingOption.fromJson(null, 11);
            fail("Should have thrown an invalid entity exception");
        } catch(InvalidEntityException e) {
            assertTrue(e.getMessage().contains("scope is required"));
        }        
    }
    
}
