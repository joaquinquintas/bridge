package org.sagebionetworks.bridge.stormpath;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Map;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.sagebionetworks.bridge.BridgeUtils;
import org.sagebionetworks.bridge.Roles;
import org.sagebionetworks.bridge.config.BridgeConfig;
import org.sagebionetworks.bridge.config.BridgeConfigFactory;
import org.sagebionetworks.bridge.dao.DirectoryDao;
import org.sagebionetworks.bridge.exceptions.BridgeServiceException;
import org.sagebionetworks.bridge.models.studies.EmailTemplate;
import org.sagebionetworks.bridge.models.studies.EmailTemplate.MimeType;
import org.sagebionetworks.bridge.models.studies.Study;
import org.sagebionetworks.bridge.validators.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import com.stormpath.sdk.application.AccountStoreMapping;
import com.stormpath.sdk.application.AccountStoreMappingCriteria;
import com.stormpath.sdk.application.AccountStoreMappings;
import com.stormpath.sdk.application.Application;
import com.stormpath.sdk.client.Client;
import com.stormpath.sdk.directory.Directories;
import com.stormpath.sdk.directory.Directory;
import com.stormpath.sdk.directory.DirectoryCriteria;
import com.stormpath.sdk.directory.DirectoryList;
import com.stormpath.sdk.directory.PasswordPolicy;
import com.stormpath.sdk.directory.PasswordStrength;
import com.stormpath.sdk.group.Group;
import com.stormpath.sdk.group.GroupCriteria;
import com.stormpath.sdk.group.GroupList;
import com.stormpath.sdk.group.Groups;
import com.stormpath.sdk.mail.EmailStatus;
import com.stormpath.sdk.mail.ModeledEmailTemplate;
import com.stormpath.sdk.mail.ModeledEmailTemplateList;

@Component
public class StormpathDirectoryDao implements DirectoryDao {

    private static final Logger logger = LoggerFactory.getLogger(StormpathDirectoryDao.class);

    private static final AccountStoreMappingCriteria asmCriteria = AccountStoreMappings.criteria().limitTo(100);
    private BridgeConfig config;
    private Client client;

    @Autowired
    public void setBridgeConfig(BridgeConfig bridgeConfig) {
        this.config = bridgeConfig;
    }
    @Autowired
    public void setStormpathClient(Client client) {
        this.client = client;
    }

    @Override
    public String createDirectoryForStudy(Study study) {
        checkNotNull(study);
        checkArgument(isNotBlank(study.getIdentifier()), Validate.CANNOT_BE_BLANK, "identifier");
        Application app = getApplication();
        checkNotNull(app);
        String dirName = createDirectoryName(study.getIdentifier());

        Directory directory = getDirectory(dirName);
        if (directory == null) {
        	System.out.println("CREATING DIR");
            directory = client.instantiate(Directory.class);
            directory.setName(dirName);
            directory = client.createDirectory(directory);
            System.out.println("CREATE THE DIR");
        }
        
        adjustPasswordPolicies(study, directory);
        adjustVerifyEmailPolicies(study, directory);
        
        AccountStoreMapping mapping = getApplicationMapping(directory.getHref(), app);
        if (mapping == null) {
            mapping = client.instantiate(AccountStoreMapping.class);
            mapping.setAccountStore(directory);
            mapping.setApplication(app);
            mapping.setDefaultAccountStore(Boolean.FALSE);
            mapping.setDefaultGroupStore(Boolean.FALSE);
            mapping.setListIndex(10); // this is a priority number
            app.createAccountStoreMapping(mapping);
        }
        for (Roles role : Roles.values()) {
            Group group = getGroup(directory, role);
            if (group == null) {
                group = client.instantiate(Group.class);
                group.setName(role.name().toLowerCase());
                directory.createGroup(group);
            }
        }
        return directory.getHref();
    }
    
    /**
     * Once a study is created, the researcher can only change the password policies, and the email templates
     * for the email verification and password reset workflows.
     */
    @Override
    public void updateDirectoryForStudy(Study study) {
        checkNotNull(study);
        
        Directory directory = getDirectoryForStudy(study.getIdentifier());
        adjustPasswordPolicies(study, directory);
        adjustVerifyEmailPolicies(study, directory);
    };

    @Override
    public Directory getDirectoryForStudy(String identifier) {
        String dirName = createDirectoryName(identifier);
        return getDirectory(dirName);
    }

    @Override
    public void deleteDirectoryForStudy(String identifier) {
        checkArgument(isNotBlank(identifier), Validate.CANNOT_BE_BLANK, "identifier");
        Application app = getApplication();
        checkNotNull(app);

        Directory existing = getDirectory(createDirectoryName(identifier));
        
        // delete the mapping
        AccountStoreMapping mapping = getApplicationMapping(existing.getHref(), app);
        if (mapping != null) {
            mapping.delete();
        } else {
            logger.warn("AccountStoreMapping not found: " + app.getName() + ", " + existing.getHref());
        }

        // delete the directory
        Directory directory = client.getResource(existing.getHref(), Directory.class);
        if (directory != null) {
            directory.delete();
        } else {
            logger.warn("Directory not found: " + existing.getHref());
        }
    }

    private Group getGroup(Directory dir, Roles role) {
        GroupCriteria criteria = Groups.where(Groups.name().eqIgnoreCase(role.name().toLowerCase()));
        GroupList list = dir.getGroups(criteria);
        return (list.iterator().hasNext()) ? list.iterator().next() : null;
    }
    
    private String createDirectoryName(String identifier) {
        return String.format("%s (%s)", identifier, config.getEnvironment().name().toLowerCase());
    }
    
    private Application getApplication() {
    	System.out.println("HREF");
    	System.out.println(config.getStormpathApplicationHref());
    	System.out.println("ID");
    	System.out.println(config.getStormpathId());
    	System.out.println("SECRET");
    	System.out.println(config.getStormpathSecret());
    	
        Application a =  client.getResource(config.getStormpathApplicationHref(), Application.class);
        a.setDescription("A new JoA description.").save();
        return a;
    }
    
    private AccountStoreMapping getApplicationMapping(String href, Application app) {
        // This is tedious but I see no way to search for or make a reference to this 
        // mapping without iterating through the application's mappings.
        for (AccountStoreMapping mapping : app.getAccountStoreMappings(asmCriteria)) {
            if (mapping.getAccountStore().getHref().equals(href)) {
                return mapping;
            }
        }
        return null;
    }

    private Directory getDirectory(String name) {
    	System.out.println("Directory name:");
    	System.out.println(name);
        DirectoryCriteria criteria = Directories.where(Directories.name().eqIgnoreCase(name));
        DirectoryList list = client.getDirectories(criteria);
        return (list.iterator().hasNext()) ? list.iterator().next() : null;
    }
    
    private void adjustPasswordPolicies(Study study, Directory directory) {
        EmailTemplate rp = study.getResetPasswordTemplate();
        
        PasswordPolicy passwordPolicy = directory.getPasswordPolicy();
        passwordPolicy.setResetEmailStatus(EmailStatus.DISABLED);
        passwordPolicy.setResetSuccessEmailStatus(EmailStatus.DISABLED);
        
        /*ModeledEmailTemplateList resetEmailTemplates = passwordPolicy.getResetEmailTemplates();
        
        // According to the documentation, there is only one...
        ModeledEmailTemplate template = resetEmailTemplates.iterator().next();
        if (study.getSponsorName() != null) {
            template.setFromName(study.getSponsorName());    
        } else {
            template.setFromName(study.getName());
        }
        
       

        String subject = partiallyResolveTemplate(rp.getSubject(), study);
        template.setSubject(subject);
        
        com.stormpath.sdk.mail.MimeType stormpathMimeType = getStormpathMimeType(study);
        template.setMimeType(stormpathMimeType);
        
        String body = partiallyResolveTemplate(rp.getBody(), study);
        //template.setTextBody(body);
        template.setHtmlBody("<p>Forgot your password?</p>"+ 
		"<p>We've received a request to reset the password for this email address.</p> "+
		"<p>To reset your password please click on this link or cut and paste this URL into your browser (link expires in 18 hours):</p>"+ 
		"<p>http://localhost:9000/mobile/resetPassword.html?study=api</p> "+
		"<p>This link takes you to a secure page where you can change your password.</p> "+
		"<p>If you don't want to reset your password, please ignore this message. Your password will not be reset.</p>"+  
		"<p>For general inquiries or to request support with your account, please email support@sagebridge.org</p>");
        System.out.println("----------");
        System.out.println("rp.getBody():");
        System.out.println(rp.getBody());
        System.out.println("study.getSponsorName():");
        System.out.println(study.getSponsorName());
        System.out.println("study.getName():");
        System.out.println(study.getName());
        System.out.println("study.getSponsorName():");
        System.out.println(study.getSponsorName());
        System.out.println("partiallyResolveTemplate(rp.getSubject(), study):");
        System.out.println(partiallyResolveTemplate(rp.getSubject(), study));
        System.out.println("getStormpathMimeType(study):");
        System.out.println(getStormpathMimeType(study));
        System.out.println("partiallyResolveTemplate(rp.getBody(), study):");
        System.out.println(partiallyResolveTemplate(rp.getBody(), study));


        template.setFromEmailAddress(study.getSupportEmail());

        String link = String.format("%s/mobile/resetPassword.html?study=%s", config.getBaseURL(), study.getIdentifier());
        System.out.println(link);
        template.setLinkBaseUrl(link);
        template.save();*/
        
        PasswordStrength strength = passwordPolicy.getStrength();
        strength.setMaxLength(org.sagebionetworks.bridge.models.studies.PasswordPolicy.FIXED_MAX_LENGTH);
        strength.setMinDiacritic(0);
        strength.setMinLength(study.getPasswordPolicy().getMinLength());
        strength.setMinNumeric(study.getPasswordPolicy().isNumericRequired() ? 1 : 0);
        strength.setMinSymbol(study.getPasswordPolicy().isSymbolRequired() ? 1 : 0);
        strength.setMinLowerCase(study.getPasswordPolicy().isLowerCaseRequired() ? 1 : 0);
        strength.setMinUpperCase(study.getPasswordPolicy().isUpperCaseRequired() ? 1 : 0);
        strength.save();
        passwordPolicy.save();
    }
    
    /**
     * There is a pull request pending to add this to the Stormpath Java SDK. So this REST/HTTP code should be temporary.
     * 
     * @param study
     * @param directory
     */
    private void adjustVerifyEmailPolicies(Study study, Directory directory) {
        try {
            EmailTemplate ve = study.getVerifyEmailTemplate();
            BridgeConfig config = BridgeConfigFactory.getConfig();
            
            // Create an HTTP client using Basic Auth with stormpath credentials (yes it's basic auth but it's over SSL and going away)
            CredentialsProvider provider = new BasicCredentialsProvider();
            System.out.print("Stormpath:");
            System.out.print(config.getStormpathId());
            System.out.print(config.getStormpathSecret());
            
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(config.getStormpathId(), config.getStormpathSecret());
            provider.setCredentials(AuthScope.ANY, credentials);
            CloseableHttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
            
            // Get directory as JSON
            ObjectNode directoryNode = StormpathUtils.getJSON(client, directory.getHref());
            String accountCreationUrl = directoryNode.get("accountCreationPolicy").get("href").asText();
            
            // Get account policy as JSON, update to our standard configuration
            ObjectNode accountPolicyNode = StormpathUtils.getJSON(client, accountCreationUrl);
            String verificationEmailTemplatesUrl = accountPolicyNode.get("verificationEmailTemplates").get("href").asText();
            accountPolicyNode.put("verificationEmailStatus", "ENABLED");
            accountPolicyNode.put("verificationSuccessEmailStatus", "DISABLED");
            accountPolicyNode.put("welcomeEmailStatus", "DISABLED");
            // save the account policy
            StormpathUtils.postJSON(client, accountCreationUrl, accountPolicyNode);
            
            // Get the verify email template
            ObjectNode templateNode = StormpathUtils.getJSON(client, verificationEmailTemplatesUrl);
            String templateUrl = templateNode.get("items").get(0).get("href").asText();
            // Update this template with study-specific information
            ObjectNode template = StormpathUtils.getJSON(client, templateUrl);
            if (study.getSponsorName() != null) {
                template.put("fromName", study.getSponsorName());    
            } else {
                template.put("fromName", study.getName());
            }
            template.put("fromEmailAddress", study.getSupportEmail());
            template.put("subject", partiallyResolveTemplate(ve.getSubject(), study));
            template.put("mimeType", ve.getMimeType() == MimeType.HTML ? "text/html" : "text/plain");
            String body = partiallyResolveTemplate(ve.getBody(), study);
            template.put("textBody", body);
            template.put("htmlBody", body);
            String link = String.format("%s/mobile/verifyEmail.html?study=%s", config.getBaseURL(), study.getIdentifier());
            ((ObjectNode)template.get("defaultModel")).put("linkBaseUrl", link);
            // save the verify email template
            StormpathUtils.postJSON(client, templateUrl, template);
        } catch(Throwable throwable) {
            throw new BridgeServiceException(throwable);
        }
    }

    private com.stormpath.sdk.mail.MimeType getStormpathMimeType(Study study) {
        return (study.getResetPasswordTemplate().getMimeType() == EmailTemplate.MimeType.TEXT) ? 
            com.stormpath.sdk.mail.MimeType.PLAIN_TEXT : com.stormpath.sdk.mail.MimeType.HTML;
    }
    
    private String partiallyResolveTemplate(String template, Study study) {
        Map<String,String> map = Maps.newHashMap();
        map.put("studyName", study.getName());
        map.put("supportEmail", study.getSupportEmail());
        map.put("technicalEmail", study.getTechnicalEmail());
        map.put("sponsorName", study.getSponsorName());
        return BridgeUtils.resolveTemplate(template, map);
    }
}
