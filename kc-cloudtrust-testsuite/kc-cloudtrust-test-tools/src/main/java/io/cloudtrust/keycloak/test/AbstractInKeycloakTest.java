package io.cloudtrust.keycloak.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudtrust.keycloak.test.container.KeycloakDeploy;
import io.cloudtrust.keycloak.test.container.KeycloakQuarkusContainer;
import io.cloudtrust.keycloak.test.container.SystemEnv;
import io.cloudtrust.keycloak.test.events.EventsManager;
import io.cloudtrust.keycloak.test.init.InjectionException;
import io.cloudtrust.keycloak.test.init.TestInitializer;
import io.cloudtrust.keycloak.test.util.ConsumerExcept;
import io.cloudtrust.keycloak.test.util.FlowUtil;
import io.cloudtrust.keycloak.test.util.JsonToolbox;
import io.cloudtrust.keycloak.test.util.OidcTokenProvider;
import org.jboss.logging.Logger;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class AbstractInKeycloakTest extends AbstractKeycloakTest {
    private static final Logger LOG = Logger.getLogger(AbstractInKeycloakTest.class);

    protected ObjectMapper mapper = new ObjectMapper();
    private String defaultRealmName = null;

    private TestInitializer testInitializer;

    /**
     * Returns the used Keycloak container... don't forget to override if you don't use KeycloakDeploy
     *
     * @return the start container
     */
    public KeycloakQuarkusContainer getContainer() {
        return KeycloakDeploy.getContainer();
    }

    public String getKeycloakURL() {
        return this.getContainer().getBaseUrl();
    }

    public void withKeycloakEnvironment(SystemEnv.RunnableEx runnable) {
        SystemEnv.withKeycloakEnvironment(runnable);
    }

    public <T> T withKeycloakEnvironment(SystemEnv.FunctionEx<T> runnable) {
        return SystemEnv.withKeycloakEnvironment(runnable);
    }

    public void createRealm(String filename) throws IOException {
        createRealm(null, filename, r -> {
        });
    }

    public void createRealm(String realmName, String filename) throws IOException {
        createRealm(realmName, filename, r -> {
        });
    }

    public void createRealm(String filename, ConsumerExcept<RealmResource, IOException> whenCreated) throws IOException {
        createRealm(null, filename, whenCreated);
    }

    public void createRealm(String realmName, String filename, ConsumerExcept<RealmResource, IOException> whenCreated) throws IOException {
        KeycloakClientProvider kcAdminCliProvider = this.getContainer().getKeycloakAdminClientProvider();
        RealmResource testRealm = importTestRealm(kcAdminCliProvider, realmName, filename);
        whenCreated.accept(testRealm);
    }

    protected RealmResource importTestRealm(KeycloakClientProvider kcAdminCliProvider, String realmName, String realmFilePath) throws IOException {
        RealmRepresentation realmRepresentation = new ObjectMapper().readValue(
                getClass().getResourceAsStream(realmFilePath), RealmRepresentation.class);
        if (realmName == null) {
            realmName = realmRepresentation.getRealm();
        }
        LOG.debugf("Creating realm %s", realmName);
        events().onRealmRemoved(realmName);
        adminEvents().onRealmRemoved(realmName);
        RealmResource realm = kcAdminCliProvider.getKeycloakAdminClient().realm(realmName);
        try {
            realm.remove();
        } catch (Exception e) {
            // Ignore
        }
        kcAdminCliProvider.getKeycloakAdminClient().realms().create(realmRepresentation);
        if (this.defaultRealmName == null) {
            this.defaultRealmName = realmName;
        }
        kcAdminCliProvider.reset(); // Force refresh of JWT with audience/roles for the new realm
        return kcAdminCliProvider.getKeycloakAdminClient().realm(realmName);
    }

    public RealmResource getRealm() {
        return getRealm(defaultRealmName);
    }

    public RealmResource getRealm(String realmName) {
        return this.getKeycloakAdminClient().realm(realmName);
    }

    public void updateRealm(String realmName, Consumer<RealmRepresentation> updater) {
        updateRealm(this.getRealm(realmName), updater);
    }

    public boolean deleteRealm() {
        return this.deleteRealm(this.defaultRealmName);
    }

    public boolean deleteRealm(String realmName) {
        try {
            LOG.debugf("Removing realm %s", realmName);
            getRealm(realmName).remove();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void disableLightweightAccessToken(String realmName, String clientId) {
        updateRealmClient(realmName, clientId, client -> {
            client.getAttributes().put("client.use.lightweight.access.token.enabled", "false");
        });
        if (getContainer().isTokenRealm(realmName)) {
            resetAdminClient();
        }
    }

    /**
     * Update the default test realm user profile
     *
     * @param updater
     */
    public void updateUserProfile(Consumer<UPConfig> updater) {
        this.updateUserProfile(this.defaultRealmName, updater);
    }

    /**
     * Update the named realm user profile
     *
     * @param realmName
     * @param updater
     */
    public void updateUserProfile(String realmName, Consumer<UPConfig> updater) {
        this.updateUserProfile(getRealm(realmName), updater);
    }

    protected void resetAdminClient() {
        getContainer().getKeycloakAdminClientProvider().reset();
    }

    public void updateRealmClient(String realmName, String clientId, Consumer<ClientRepresentation> updater) {
        updateRealmClient(this.getRealm(realmName), clientId, updater);
    }

    public void updateIdentityProvider(String idpAlias, Consumer<IdentityProviderRepresentation> updater) {
        this.updateIdentityProvider(defaultRealmName, idpAlias, updater);
    }

    public void updateIdentityProvider(String realmName, String idpAlias, Consumer<IdentityProviderRepresentation> updater) {
        this.updateIdentityProvider(this.getRealm(realmName), idpAlias, updater);
    }

    public List<UserRepresentation> searchUsers(String username) {
        return searchUsers(defaultRealmName, username);
    }

    public List<UserRepresentation> searchUsers(String realmName, String username) {
        return searchUsers(getRealm(realmName), username);
    }

    public UserRepresentation getUserByName(String username) {
        return getUserByName(this.defaultRealmName, username);
    }

    public UserRepresentation getUserByName(String realmName, String username) {
        return getUserByName(getRealm(realmName), username);
    }

    public Map<String, List<String>> getUserAttributes(String username) {
        return getUserAttributes(defaultRealmName, username);
    }

    public Map<String, List<String>> getUserAttributes(String realmName, String username) {
        return getUserAttributes(getRealm(realmName), username);
    }

    public List<String> getUserAttribute(String username, String attributeName) {
        return getUserAttribute(defaultRealmName, username, attributeName);
    }

    public List<String> getUserAttribute(String realmName, String username, String attributeName) {
        return this.getUserAttribute(getRealm(realmName), username, attributeName);
    }

    public String getUserAttributeAsString(String username, String attributeName) {
        return getUserAttributeAsString(defaultRealmName, username, attributeName);
    }

    public String getUserAttributeAsString(String realmName, String username, String attributeName) {
        return getUserAttributeAsString(getRealm(realmName), username, attributeName);
    }

    public int getUserAttributeAsInt(String username, String attributeName) {
        return getUserAttributeAsInt(defaultRealmName, username, attributeName);
    }

    public int getUserAttributeAsInt(String realmName, String username, String attributeName) {
        return getUserAttributeAsInt(getRealm(realmName), username, attributeName);
    }

    public void setUserAttribute(String username, String attributeName, List<String> values) {
        setUserAttribute(defaultRealmName, username, attributeName, values);
    }

    public void setUserAttribute(String realmName, String username, String attributeName, List<String> values) {
        setUserAttribute(getRealm(realmName), username, attributeName, values);
    }

    public void removeUserAttribute(String username, String attributeName) {
        removeUserAttribute(defaultRealmName, username, attributeName);
    }

    public void removeUserAttribute(String realmName, String username, String attributeName) {
        removeUserAttribute(getRealm(realmName), username, attributeName);
    }

    public Stream<CredentialRepresentation> getUserCredentials(String username, Predicate<? super CredentialRepresentation> predicate) {
        return getUserCredentials(defaultRealmName, username, predicate);
    }

    public Stream<CredentialRepresentation> getUserCredentials(String realmName, String username, Predicate<? super CredentialRepresentation> predicate) {
        return getUserCredentials(getRealm(realmName), username, predicate);
    }

    public void registerRequiredActions(String realmName, String... requiredActionIds) {
        registerRequiredActions(getRealm(realmName), requiredActionIds);
    }

    public void setBrowserFlow(String flow) {
        setBrowserFlow(defaultRealmName, flow);
    }

    public void setBrowserFlow(String realmName, String flow) {
        RealmResource testRealm = getRealm(realmName);
        RealmRepresentation realm = testRealm.toRepresentation();
        realm.setBrowserFlow(flow);
        testRealm.update(realm);
    }

    /**
     * Creates an enabled user with email=username+"@test.com" and password="password+"
     *
     * @param username    Username
     * @param userUpdater Lambda used to customize the user before it is created
     */
    protected String createUser(String username, Consumer<UserRepresentation> userUpdater) {
        return this.createUser(this.getRealm(), username, userUpdater);
    }

    protected Keycloak getKeycloakAdminClient() {
        return this.getContainer().getKeycloakAdminClientProvider().getKeycloakAdminClient();
    }

    protected FlowUtil getFlowUtil() {
        return getFlowUtil(defaultRealmName);
    }

    protected FlowUtil getFlowUtil(String realmName) {
        return getFlowUtil(getRealm(realmName));
    }

    /**
     * Events management
     */
    public EventsManager<EventRepresentation> events() {
        return this.getContainer().getKeycloakAdminClientProvider().events();
    }

    public EventsManager<AdminEventRepresentation> adminEvents() {
        return this.getContainer().getKeycloakAdminClientProvider().adminEvents();
    }

    /**
     * API management
     */
    protected ExtensionApi api() {
        return this.getContainer().getKeycloakAdminClientProvider().api();
    }

    public OidcTokenProvider createOidcTokenProvider() {
        return createOidcTokenProvider("VPN", "VPN-CLIENT-SECRET");
    }

    public OidcTokenProvider createOidcTokenProvider(String username, String password) {
        return createOidcTokenProvider(defaultRealmName, username, password);
    }

    public OidcTokenProvider createOidcTokenProvider(String realm, String username, String password) {
        return new OidcTokenProvider(getKeycloakURL(), "/realms/" + realm + "/protocol/openid-connect/token", username, password);
    }

    public void injectComponents() throws InjectionException {
        injectComponents(false);
    }

    public void injectComponents(boolean forceReInit) throws InjectionException {
        if (this.testInitializer == null) {
            this.testInitializer = new TestInitializer();
        }
        testInitializer.init(this, forceReInit);
    }

    public void logObject(String message, Object obj) {
        LOG.debugf("%s: %s", message, JsonToolbox.toString(obj));
    }
}