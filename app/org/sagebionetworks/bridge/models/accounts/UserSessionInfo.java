package org.sagebionetworks.bridge.models.accounts;

import java.util.HashMap;

import java.util.Map;
import java.util.Set;

import org.sagebionetworks.bridge.Roles;
import org.sagebionetworks.bridge.config.Environment;
import org.sagebionetworks.bridge.dao.ParticipantOption.SharingScope;

/**
 * Greatly trimmed user session object that is embedded in the initial render of the
 * web application.
 *
 */
public class UserSessionInfo {
    
    private static final Map<Environment,String> ENVIRONMENTS = new HashMap<>();
    static {
        ENVIRONMENTS.put(Environment.LOCAL, "local");
        ENVIRONMENTS.put(Environment.DEV, "develop");
        ENVIRONMENTS.put(Environment.UAT, "staging");
        ENVIRONMENTS.put(Environment.PROD, "production");
    }

    private final boolean authenticated;
    private final boolean signedMostRecentConsent;
    private final boolean consented;
    private final SharingScope sharingScope;
    private final String sessionToken;
    private final String username;
    private final String environment;
    private final Set<Roles> roles;

    public UserSessionInfo(UserSession session) {
        this.authenticated = session.isAuthenticated();
        this.sessionToken = session.getSessionToken();
        this.signedMostRecentConsent = session.getUser().hasSignedMostRecentConsent();
        this.consented = session.getUser().doesConsent();
        this.sharingScope = session.getUser().getSharingScope();
        this.username = session.getUser().getUsername();
        this.roles = session.getUser().getRoles();
        this.environment = ENVIRONMENTS.get(session.getEnvironment());
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
    public boolean isConsented() {
        return consented;
    }
    public boolean isSignedMostRecentConsent() {
        return signedMostRecentConsent;
    }
    public SharingScope getSharingScope() {
        return sharingScope;
    }
    public boolean isDataSharing() {
        return (sharingScope != SharingScope.NO_SHARING);
    }
    public String getSessionToken() {
        return sessionToken;
    }
    public String getUsername() {
        return username;
    }
    public String getEnvironment() {
        return environment;
    }
    public Set<Roles> getRoles() {
        return roles;
    }
}
