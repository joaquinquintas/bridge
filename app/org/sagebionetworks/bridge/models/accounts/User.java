package org.sagebionetworks.bridge.models.accounts;

import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.bridge.Roles;
import org.sagebionetworks.bridge.config.BridgeConfigFactory;
import org.sagebionetworks.bridge.crypto.AesGcmEncryptor;
import org.sagebionetworks.bridge.crypto.Encryptor;
import org.sagebionetworks.bridge.dao.ParticipantOption.SharingScope;
import org.sagebionetworks.bridge.json.BridgeTypeName;
import org.sagebionetworks.bridge.models.BridgeEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Sets;

@BridgeTypeName("User")
public class User implements BridgeEntity {

    private static final Encryptor encryptor = new AesGcmEncryptor(
            BridgeConfigFactory.getConfig().getProperty("bridge.healthcode.redis.key"));

    private String id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String healthCode;
    private String studyKey;
    private boolean consent;
    private boolean signedMostRecentConsent;
    private SharingScope sharingScope;
    private Set<Roles> roles = Sets.newHashSet();

    public User() {
    }

    public User(Account account) {
        this.email = account.getEmail();
        this.username = account.getUsername();
        this.firstName = account.getFirstName();
        this.lastName = account.getLastName();
        this.id = account.getId();
        this.roles = new HashSet<>(account.getRoles());
    }

    public User(String id, String email) {
        setId(id);
        setEmail(email);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @JsonIgnore
    public String getHealthCode() {
        return healthCode;
    }

    @JsonIgnore
    public void setHealthCode(String healthCode) {
        this.healthCode = healthCode;
    }

    public String getEncryptedHealthCode() {
        return encryptor.encrypt(healthCode);
    }

    public void setEncryptedHealthCode(String healthCode) {
        this.healthCode = encryptor.decrypt(healthCode);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStudyKey() {
        return studyKey;
    }

    public void setStudyKey(String studyKey) {
        this.studyKey = studyKey;
    }

    public Set<Roles> getRoles() {
        return roles;
    }

    public void setRoles(Set<Roles> roles) {
        this.roles = roles;
    }

    public boolean doesConsent() {
        return consent;
    }

    // Jackson serialization needs this method, even though linguistically, it makes no sense.
    public boolean isConsent() {
        return consent;
    }

    public void setConsent(boolean consent) {
        this.consent = consent;
    }

    public boolean hasSignedMostRecentConsent() {
        return this.signedMostRecentConsent;
    }

    // Need "is" for Jackson serialization.
    public boolean isSignedMostRecentConsent() {
        return this.signedMostRecentConsent;
    }

    public void setSignedMostRecentConsent(boolean signedMostRecentConsent) {
        this.signedMostRecentConsent = signedMostRecentConsent;
    }

    public SharingScope getSharingScope() {
        return sharingScope;
    }

    public void setSharingScope(SharingScope sharingScope) {
        this.sharingScope = sharingScope;
    }

    public boolean isInRole(Roles role) {
        return this.roles.contains(role);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (consent ? 1231 : 1237);
        result = prime * result + ((email == null) ? 0 : email.hashCode());
        result = prime * result + ((firstName == null) ? 0 : firstName.hashCode());
        result = prime * result + ((healthCode == null) ? 0 : healthCode.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((lastName == null) ? 0 : lastName.hashCode());
        result = prime * result + ((roles == null) ? 0 : roles.hashCode());
        result = prime * result + ((sharingScope == null) ? 0 : sharingScope.hashCode());
        result = prime * result + (signedMostRecentConsent ? 1231 : 1237);
        result = prime * result + ((studyKey == null) ? 0 : studyKey.hashCode());
        result = prime * result + ((username == null) ? 0 : username.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        User other = (User) obj;
        if (consent != other.consent)
            return false;
        if (email == null) {
            if (other.email != null)
                return false;
        } else if (!email.equals(other.email))
            return false;
        if (firstName == null) {
            if (other.firstName != null)
                return false;
        } else if (!firstName.equals(other.firstName))
            return false;
        if (healthCode == null) {
            if (other.healthCode != null)
                return false;
        } else if (!healthCode.equals(other.healthCode))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (lastName == null) {
            if (other.lastName != null)
                return false;
        } else if (!lastName.equals(other.lastName))
            return false;
        if (roles == null) {
            if (other.roles != null)
                return false;
        } else if (!roles.equals(other.roles))
            return false;
        if (sharingScope != other.sharingScope)
            return false;
        if (signedMostRecentConsent != other.signedMostRecentConsent)
            return false;
        if (studyKey == null) {
            if (other.studyKey != null)
                return false;
        } else if (!studyKey.equals(other.studyKey))
            return false;
        if (username == null) {
            if (other.username != null)
                return false;
        } else if (!username.equals(other.username))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "User [id=" + id + ", username=" + username + ", firstName=" + firstName + ", lastName=" + lastName
                + ", email=" + email + ", studyKey=" + studyKey + ", consent=" + consent + ", roles=" + roles + "]";
    }
}
