package io.cloudtrust.keycloak.test;

import io.cloudtrust.exception.CloudtrustRuntimeException;
import io.cloudtrust.keycloak.test.http.HttpServerManager;
import io.cloudtrust.keycloak.test.util.FlowUtil;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.ui.page.AbstractPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class AbstractKeycloakTest {
    private static final Logger LOG = Logger.getLogger(AbstractKeycloakTest.class);

    private String defaultUserPassword = "password+";

    protected HttpServerManager http() {
        return HttpServerManager.getDefault();
    }

    @BeforeEach
    void displayTestInformation(TestInfo testInfo) {
        String message = "Running test " + testInfo.getDisplayName();
        String separator = StringUtils.repeat('*', message.length() + 5);
        LOG.info(separator);
        LOG.info(message);
        LOG.info(separator);
    }

    // Can be used to inject a specific Selenium webdriver in a class rather than the default one
    protected void inject(Field field, WebDriver driver) {
        try {
            // field is instanceof AbstractPage ?
            if (AbstractPage.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                Object pageInstance = field.get(this);
                if (pageInstance != null) {
                    Field webDriverField = AbstractPage.class.getDeclaredField("driver");
                    webDriverField.setAccessible(true);

                    Constructor<?> constructor = field.getType().getDeclaredConstructor(WebDriver.class);
                    constructor.setAccessible(true);
                    Object newInstance = constructor.newInstance(driver);
                    PageFactory.initElements(driver, newInstance);
                    field.set(this, newInstance);
                    LOG.debugf("** WebDriver> replacing new instance of %s to field %s", field.getType().getSimpleName(), field.getName());
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to inject webdriver", e);
            throw new CloudtrustRuntimeException("Failed to inject webdriver", e);
        }
    }

    // Can be used to inject a specific Selenium webdriver in a class rather than the default one
    public <T> void injectWebDriverInPages(WebDriver driver) {
        injectWebDriverInPages(this.getClass(), driver);
    }

    public <T> void injectWebDriverInPages(Class<T> clazz, WebDriver driver) {
        if (clazz != null) {
            LOG.debugf("*** WebDriver> injecting from %s", clazz.getSimpleName());
            // Parcours tous les champs de la classe de test (this)
            for (Field field : clazz.getDeclaredFields()) {
                inject(field, driver);
            }
            injectWebDriverInPages(clazz.getSuperclass(), driver);
        }
    }

    /**
     * Gets the default user password
     */
    protected String getDefaultUserPassword() {
        return defaultUserPassword;
    }

    /**
     * Sets the default user password
     */
    protected void setDefaultUserPassword(String defaultUserPassword) {
        this.defaultUserPassword = defaultUserPassword;
    }

    /**
     * Update a realm
     * @param realm
     * @param updater
     */
    public void updateRealm(ManagedRealm realm, Consumer<RealmRepresentation> updater) {
        updateRealm(realm.admin(), updater);
    }

    public void updateRealm(RealmResource realm, Consumer<RealmRepresentation> updater) {
        RealmRepresentation realmRep = realm.toRepresentation();
        updater.accept(realmRep);
        realm.update(realmRep);
    }

    /**
     * Update the default test realm user profile
     *
     * @param realm
     * @param updater
     */
    public void updateUserProfile(ManagedRealm realm, Consumer<UPConfig> updater) {
        updateUserProfile(realm.admin(), updater);
    }

    public void updateUserProfile(RealmResource realm, Consumer<UPConfig> updater) {
        UserProfileResource userProfile = realm.users().userProfile();
        UPConfig upConfig = userProfile.getConfiguration();
        updater.accept(upConfig);
        userProfile.update(upConfig);
    }

    /**
     * Update a client
     *
     * @param realm
     * @param clientId
     * @param updater
     */
    public void updateRealmClient(ManagedRealm realm, String clientId, Consumer<ClientRepresentation> updater) {
        updateRealmClient(realm.admin(), clientId, updater);
    }

    public void updateRealmClient(RealmResource realm, String clientId, Consumer<ClientRepresentation> updater) {
        ClientRepresentation clientRep = realm.clients().findByClientId(clientId).get(0);
        ClientResource clientRes = realm.clients().get(clientRep.getId());
        updater.accept(clientRep);
        clientRes.update(clientRep);
    }

    /**
     * Update an identity provider
     *
     * @param realm
     * @param idpAlias
     * @param updater
     */
    public void updateIdentityProvider(ManagedRealm realm, String idpAlias, Consumer<IdentityProviderRepresentation> updater) {
        updateIdentityProvider(realm.admin(), idpAlias, updater);
    }

    public void updateIdentityProvider(RealmResource realm, String idpAlias, Consumer<IdentityProviderRepresentation> updater) {
        IdentityProviderResource idpRes = realm.identityProviders().get(idpAlias);
        IdentityProviderRepresentation idp = idpRes.toRepresentation();
        updater.accept(idp);
        idpRes.update(idp);
    }

    /**
     * Search users
     *
     * @param realm
     * @param username
     * @return
     */
    public List<UserRepresentation> searchUsers(ManagedRealm realm, String username) {
        return searchUsers(realm.admin(), username);
    }

    public List<UserRepresentation> searchUsers(RealmResource realm, String username) {
        return realm.users().search(username);
    }

    /**
     * Get user by name
     *
     * @param realm
     * @param username
     * @return
     */
    public UserRepresentation getUserByName(ManagedRealm realm, String username) {
        return getUserByName(realm.admin(), username);
    }

    public UserRepresentation getUserByName(RealmResource realm, String username) {
        List<UserRepresentation> users = searchUsers(realm, username);
        return users == null || users.isEmpty() ? null : users.get(0);
    }

    /**
     * Get user attributes
     *
     * @param realm
     * @param username
     * @return
     */
    public Map<String, List<String>> getUserAttributes(ManagedRealm realm, String username) {
        return getUserAttributes(realm.admin(), username);
    }

    public Map<String, List<String>> getUserAttributes(RealmResource realm, String username) {
        return searchUsers(realm, username).get(0).getAttributes();
    }

    /**
     * Get user attribute
     *
     * @param realm
     * @param username
     * @param attributeName
     * @return
     */
    public List<String> getUserAttribute(ManagedRealm realm, String username, String attributeName) {
        return getUserAttribute(realm.admin(), username, attributeName);
    }

    public List<String> getUserAttribute(RealmResource realm, String username, String attributeName) {
        Map<String, List<String>> attrbs = getUserAttributes(realm, username);
        List<String> res = attrbs == null ? null : attrbs.get(attributeName);
        return res == null ? Collections.emptyList() : res;
    }

    /**
     * Get user attribute as a string
     *
     * @param realm
     * @param username
     * @param attributeName
     * @return
     */
    public String getUserAttributeAsString(ManagedRealm realm, String username, String attributeName) {
        return getUserAttributeAsString(realm.admin(), username, attributeName);
    }

    public String getUserAttributeAsString(RealmResource realm, String username, String attributeName) {
        List<String> attrbs = getUserAttribute(realm, username, attributeName);
        return attrbs == null || attrbs.isEmpty() ? null : attrbs.get(0);
    }

    /**
     * Get user attribute as an integer
     *
     * @param realm
     * @param username
     * @param attributeName
     * @return
     */
    public int getUserAttributeAsInt(ManagedRealm realm, String username, String attributeName) {
        return getUserAttributeAsInt(realm.admin(), username, attributeName);
    }

    public int getUserAttributeAsInt(RealmResource realm, String username, String attributeName) {
        try {
            return Integer.parseInt(getUserAttributeAsString(realm, username, attributeName));
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Set a user attribute
     *
     * @param realm
     * @param username
     * @param attributeName
     * @param values
     */
    public void setUserAttribute(ManagedRealm realm, String username, String attributeName, List<String> values) {
        setUserAttribute(realm.admin(), username, attributeName, values);
    }

    public void setUserAttribute(RealmResource realm, String username, String attributeName, List<String> values) {
        UsersResource kcUsers = realm.users();
        UserRepresentation user = kcUsers.search(username).get(0);
        if (user.getAttributes() == null) {
            user.setAttributes(new HashMap<>());
        }
        user.getAttributes().put(attributeName, values);
        kcUsers.get(user.getId()).update(user);
    }

    /**
     * Remove a user attribute
     *
     * @param realm
     * @param username
     * @param attributeName
     */
    public void removeUserAttribute(ManagedRealm realm, String username, String attributeName) {
        removeUserAttribute(realm.admin(), username, attributeName);
    }

    public void removeUserAttribute(RealmResource realm, String username, String attributeName) {
        UsersResource kcUsers = realm.users();
        UserRepresentation user = kcUsers.search(username).get(0);
        user.getAttributes().remove(attributeName);
        kcUsers.get(user.getId()).update(user);
    }

    /**
     * Get user credentials
     *
     * @param realm
     * @param username
     * @param predicate
     * @return
     */
    public Stream<CredentialRepresentation> getUserCredentials(ManagedRealm realm, String username, Predicate<? super CredentialRepresentation> predicate) {
        return getUserCredentials(realm.admin(), username, predicate);
    }

    public Stream<CredentialRepresentation> getUserCredentials(RealmResource realm, String username, Predicate<? super CredentialRepresentation> predicate) {
        return realm.users().get(realm.users().search(username).get(0).getId()).credentials().stream()
                .filter(predicate);
    }

    /**
     * Register required action
     *
     * @param realm
     * @param requiredActionIds
     */
    public void registerRequiredActions(ManagedRealm realm, String... requiredActionIds) {
        registerRequiredActions(realm.admin(), requiredActionIds);
    }

    public void registerRequiredActions(RealmResource realm, String... requiredActionIds) {
        AuthenticationManagementResource flows = realm.flows();
        List<String> reqActions = Arrays.asList(requiredActionIds);
        flows.getUnregisteredRequiredActions().stream()
                .filter(ra -> reqActions.contains(ra.getProviderId()))
                .forEach(flows::registerRequiredAction);
    }

    /**
     * Create a user
     *
     * @param realm
     * @param username
     * @param userUpdater
     * @return
     */
    protected String createUser(ManagedRealm realm, String username, Consumer<UserRepresentation> userUpdater) {
        return createUser(realm.admin(), username, userUpdater);
    }

    protected String createUser(RealmResource realm, String username, Consumer<UserRepresentation> userUpdater) {
        CredentialRepresentation credentialPass = new CredentialRepresentation();
        credentialPass.setType(CredentialRepresentation.PASSWORD);
        credentialPass.setValue(defaultUserPassword);

        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setEnabled(true);
        user.setCredentials(new LinkedList<>());
        user.getCredentials().add(credentialPass);
        user.setAttributes(new HashMap<>());
        userUpdater.accept(user);
        try (Response createUser = realm.users().create(user)) {
            Assertions.assertEquals(201, createUser.getStatus());
            String location = createUser.getHeaderString("Location");
            return location.substring(location.lastIndexOf("/") + 1);
        }
    }

    protected FlowUtil getFlowUtil(ManagedRealm realm) {
        return getFlowUtil(realm.admin());
    }

    protected FlowUtil getFlowUtil(RealmResource realm) {
        return FlowUtil.inCurrentRealm(realm);
    }

    public ClientResource findClientResourceById(ManagedRealm realm, String id) {
        return findClientResourceById(realm.admin(), id);
    }

    public ClientResource findClientResourceById(RealmResource realm, String id) {
        for (ClientRepresentation c : realm.clients().findAll()) {
            if (c.getId().equals(id)) {
                return realm.clients().get(c.getId());
            }
        }
        return null;
    }

    public List<AdminEventRepresentation> pollEvents(AdminEvents events, int nb) {
        return pollEvents(events, nb, 0);
    }

    public List<AdminEventRepresentation> pollEvents(AdminEvents events, int nb, long timeout) {
        long limit = System.currentTimeMillis() + timeout;
        List<AdminEventRepresentation> res = new ArrayList<>();
        do {
            AdminEventRepresentation e = events.poll();
            if (e != null) {
                res.add(e);
            }
        } while (System.currentTimeMillis() < limit && res.size() < nb);
        return res;
    }

    public List<EventRepresentation> pollEvents(Events events, int nb) {
        return pollEvents(events, nb, 0);
    }

    public List<EventRepresentation> pollEvents(Events events, int nb, long timeout) {
        long limit = System.currentTimeMillis() + timeout;
        List<EventRepresentation> res = new ArrayList<>();
        do {
            EventRepresentation e = events.poll();
            if (e != null) {
                res.add(e);
            }
        } while (System.currentTimeMillis() < limit && res.size() < nb);
        return res;
    }

    public ClientResource findClientByClientId(ManagedRealm realm, String clientId) {
        return findClientByClientId(realm.admin(), clientId);
    }

    public ClientResource findClientByClientId(RealmResource realm, String clientId) {
        for (ClientRepresentation c : realm.clients().findAll()) {
            if (clientId.equals(c.getClientId())) {
                return realm.clients().get(c.getId());
            }
        }
        return null;
    }

    protected void sleep(Duration duration) {
        this.sleep(duration.toMillis());
    }

    protected void sleep(long duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ie) {
            // Ignore exception
        }
    }

    protected void sleep(Duration maxDuration, Duration interval, BooleanSupplier shouldStop) {
        this.sleep(maxDuration.toMillis(), interval.toMillis(), shouldStop);
    }

    protected boolean sleep(Duration maxDuration, long interval, BooleanSupplier shouldStop) {
        return this.sleep(maxDuration.toMillis(), interval, shouldStop);
    }

    protected boolean sleep(Duration maxDuration, BooleanSupplier shouldStop) {
        return sleep(maxDuration.toMillis(), 100, shouldStop);
    }

    protected boolean sleep(long maxDuration, BooleanSupplier shouldStop) {
        return sleep(maxDuration, 100, shouldStop);
    }

    /**
     * Waits until a given condition is true. Uses a maximum time limit.
     *
     * @param conditionWhile
     * @param maxDelay
     * @throws InterruptedException
     */
    protected void waitWhile(BooleanSupplier conditionWhile, long maxDelay) throws InterruptedException {
        waitWhile(conditionWhile, 500, maxDelay);
    }

    protected void waitWhile(BooleanSupplier conditionWhile, long interval, long maxDelay) throws InterruptedException {
        long maxTime = System.currentTimeMillis() + maxDelay;
        while (conditionWhile.getAsBoolean()) {
            this.sleep(interval);
            if (System.currentTimeMillis() > maxTime) {
                Assertions.fail();
            }
        }
    }

    /**
     * Sleep for the given maxDuration. Stops if provided supplier is true
     *
     * @param maxDuration Milliseconds
     * @param interval    Interval between supplier evaluation
     * @param shouldStop  Supplier which returns true if the sleep period can be interrupted
     * @return True if supplier told to stop, false in case of timeout
     */
    protected boolean sleep(long maxDuration, long interval, BooleanSupplier shouldStop) {
        try {
            long limit = System.currentTimeMillis() + maxDuration;
            while (System.currentTimeMillis() < limit) {
                long maxPause = Math.max(0, limit - System.currentTimeMillis());
                Thread.sleep(Math.min(interval, maxPause));
                if (shouldStop.getAsBoolean()) {
                    return true;
                }
            }
        } catch (InterruptedException ie) {
            // Ignore exception
        }
        return false;
    }

    public ExtensionApi api(Keycloak keycloak, ManagedRealm realm) {
        String baseUrl = realm.getBaseUrl();
        baseUrl = baseUrl.substring(0, baseUrl.indexOf("/realms/"));
        return new ExtensionApi(baseUrl, () -> keycloak.tokenManager().getAccessTokenString());
    }

}
