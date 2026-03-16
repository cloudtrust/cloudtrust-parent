package io.cloudtrust.keycloak.test;

import io.cloudtrust.exception.CloudtrustRuntimeException;
import io.cloudtrust.keycloak.test.http.HttpServerManager;
import io.cloudtrust.keycloak.test.util.FlowUtil;
import io.cloudtrust.keycloak.test.util.OAuthClient;
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

    protected String getKeycloakURL(ManagedRealm realm) {
        var res = realm.getBaseUrl();
        return res.substring(0, res.indexOf("/realms/"));
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

    protected String getLoginFormUrl(ManagedRealm realm) {
        return new OAuthClient(realm.getBaseUrl()).getLoginFormUrl();
    }

    protected void openLoginForm(WebDriver webDriver, ManagedRealm realm) {
        webDriver.navigate().to(getLoginFormUrl(realm));
    }

    protected String getLogoutUrl(ManagedRealm realm) {
        return new OAuthClient(realm.getBaseUrl()).getLogoutFormUrl();
    }

    protected void openLogout(WebDriver webDriver, ManagedRealm realm) {
        webDriver.navigate().to(getLogoutUrl(realm));
    }

    /**
     * Update a realm
     *
     * @param realm   Realm to update
     * @param updater Consumer that takes a RealmRepresentation, modifies it and returns it
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
     * @param realm   Realm to update
     * @param updater Consumer that takes a UPConfig, modifies it and returns it
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
     * @param realm    Realm to update
     * @param clientId ID of the client to update
     * @param updater  Consumer that takes a ClientRepresentation, modifies it and returns it
     */
    public void updateRealmClient(ManagedRealm realm, String clientId, Consumer<ClientRepresentation> updater) {
        updateRealmClient(realm.admin(), clientId, updater);
    }

    public void updateRealmClient(RealmResource realm, String clientId, Consumer<ClientRepresentation> updater) {
        ClientRepresentation clientRep = realm.clients().findByClientId(clientId).getFirst();
        ClientResource clientRes = realm.clients().get(clientRep.getId());
        updater.accept(clientRep);
        clientRes.update(clientRep);
    }

    /**
     * Update an identity provider
     *
     * @param realm    Realm to update
     * @param idpAlias Alias of the identity provider to update
     * @param updater  Consumer that takes an IdentityProviderRepresentation, modifies it and returns it
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
     * @param realm    Realm to search in
     * @param username Username to search for
     * @return A list of UserRepresentation matching the search criteria
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
     * @param realm    Realm to search in
     * @param username Username of the user to search for
     * @return UserRepresentation of the user with the given username, or null if no such user is found
     */
    public UserRepresentation getUserByName(ManagedRealm realm, String username) {
        return getUserByName(realm.admin(), username);
    }

    public UserRepresentation getUserByName(RealmResource realm, String username) {
        List<UserRepresentation> users = searchUsers(realm, username);
        return users == null || users.isEmpty() ? null : users.getFirst();
    }

    /**
     * Get user attributes
     *
     * @param realm    Realm to search in
     * @param username Username of the user
     * @return A map of user attributes, where the key is the attribute name and the value is a list of strings representing the attribute values.
     */
    public Map<String, List<String>> getUserAttributes(ManagedRealm realm, String username) {
        return getUserAttributes(realm.admin(), username);
    }

    public Map<String, List<String>> getUserAttributes(RealmResource realm, String username) {
        return searchUsers(realm, username).getFirst().getAttributes();
    }

    /**
     * Get user attribute
     *
     * @param realm         Realm to search in
     * @param username      Username of the user
     * @param attributeName Name of the attribute to get
     * @return The attribute value as a list of strings, or an empty list if the attribute is not found
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
     * @param realm         Realm to search in
     * @param username      Username of the user
     * @param attributeName Name of the attribute to get. If the attribute has multiple values, only the first one will be returned.
     * @return The attribute value as a string, or null if the attribute is not found
     */
    public String getUserAttributeAsString(ManagedRealm realm, String username, String attributeName) {
        return getUserAttributeAsString(realm.admin(), username, attributeName);
    }

    public String getUserAttributeAsString(RealmResource realm, String username, String attributeName) {
        List<String> attrbs = getUserAttribute(realm, username, attributeName);
        return attrbs == null || attrbs.isEmpty() ? null : attrbs.getFirst();
    }

    /**
     * Get user attribute as an integer
     *
     * @param realm         Realm to search in
     * @param username      Username of the user
     * @param attributeName Name of the attribute to get. The attribute value must be an integer, otherwise 0 will be returned.
     * @return The attribute value as an integer, or 0 if the attribute is not found or cannot be parsed as an integer
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
     * @param realm         Realm to update
     * @param username      Username of the user to update
     * @param attributeName Name of the attribute to set
     * @param values        Values of the attribute to set
     */
    public void setUserAttribute(ManagedRealm realm, String username, String attributeName, List<String> values) {
        setUserAttribute(realm.admin(), username, attributeName, values);
    }

    public void setUserAttribute(RealmResource realm, String username, String attributeName, List<String> values) {
        UsersResource kcUsers = realm.users();
        UserRepresentation user = kcUsers.search(username).getFirst();
        if (user.getAttributes() == null) {
            user.setAttributes(new HashMap<>());
        }
        user.getAttributes().put(attributeName, values);
        kcUsers.get(user.getId()).update(user);
    }

    /**
     * Remove a user attribute
     *
     * @param realm         Realm to update
     * @param username      Username of the user to update
     * @param attributeName Name of the attribute to remove
     */
    public void removeUserAttribute(ManagedRealm realm, String username, String attributeName) {
        removeUserAttribute(realm.admin(), username, attributeName);
    }

    public void removeUserAttribute(RealmResource realm, String username, String attributeName) {
        UsersResource kcUsers = realm.users();
        UserRepresentation user = kcUsers.search(username).getFirst();
        user.getAttributes().remove(attributeName);
        kcUsers.get(user.getId()).update(user);
    }

    /**
     * Get user credentials
     *
     * @param realm     Realm to search in
     * @param username  Username of the user
     * @param predicate Predicate to filter credentials. All credentials for which the predicate returns true will be returned.
     * @return A stream of credentials matching the predicate
     */
    public Stream<CredentialRepresentation> getUserCredentials(ManagedRealm realm, String username, Predicate<? super CredentialRepresentation> predicate) {
        return getUserCredentials(realm.admin(), username, predicate);
    }

    public Stream<CredentialRepresentation> getUserCredentials(RealmResource realm, String username, Predicate<? super CredentialRepresentation> predicate) {
        return realm.users().get(realm.users().search(username).getFirst().getId()).credentials().stream()
                .filter(predicate);
    }

    /**
     * Register required action
     *
     * @param realm             Realm to update
     * @param requiredActionIds IDs of required actions to register
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
     * @param realm       Realm to update
     * @param username    Username of the user to create
     * @param userUpdater Consumer that takes a UserRepresentation, modifies it and returns it.
     * @return The ID of the created user
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

    public AdminEventRepresentation poll(AdminEvents events, Duration timeout) {
        return poll(events, null, timeout);
    }

    public AdminEventRepresentation poll(AdminEvents events, Predicate<AdminEventRepresentation> eventPredicate, Duration timeout) {
        return poll(events, eventPredicate, timeout.toMillis());
    }

    public AdminEventRepresentation poll(AdminEvents events, Predicate<AdminEventRepresentation> eventPredicate, long timeoutMillis) {
        var res = pollEvents(events, 1, eventPredicate, timeoutMillis);
        return res.isEmpty() ? null : res.getFirst();
    }

    public List<AdminEventRepresentation> pollEvents(AdminEvents events, int nb) {
        return pollEvents(events, nb, e -> true);
    }

    public List<AdminEventRepresentation> pollEvents(AdminEvents events, int nb, Predicate<AdminEventRepresentation> eventPredicate) {
        return pollEvents(events, nb, eventPredicate, 0);
    }

    public List<AdminEventRepresentation> pollEvents(AdminEvents events, int nb, Duration timeout) {
        return pollEvents(events, nb, e -> true, timeout.toMillis());
    }

    public List<AdminEventRepresentation> pollEvents(AdminEvents events, int nb, long timeoutMillis) {
        return pollEvents(events, nb, e -> true, timeoutMillis);
    }

    public List<AdminEventRepresentation> pollEvents(AdminEvents events, int nb, Predicate<AdminEventRepresentation> eventPredicate, long timeoutMillis) {
        long limit = System.currentTimeMillis() + timeoutMillis;
        List<AdminEventRepresentation> res = new ArrayList<>();
        do {
            AdminEventRepresentation e = events.poll();
            if (e != null && (eventPredicate==null || eventPredicate.test(e))) {
                res.add(e);
            }
        } while (System.currentTimeMillis() < limit && res.size() < nb);
        return res;
    }

    public EventRepresentation poll(Events events, Duration timeout) {
        return poll(events, timeout, null);
    }

    public EventRepresentation poll(Events events, Duration timeout, Predicate<EventRepresentation> eventPredicate) {
        var res = pollEvents(events, 1, eventPredicate, timeout);
        return res.isEmpty() ? null : res.getFirst();
    }

    public List<EventRepresentation> pollEvents(Events events, int nb) {
        return pollEvents(events, nb, null,0);
    }

    public List<EventRepresentation> pollEvents(Events events, int nb, Predicate<EventRepresentation> eventPredicate) {
        return pollEvents(events, nb, eventPredicate, 0);
    }

    public List<EventRepresentation> pollEvents(Events events, int nb, Duration timeout) {
        return pollEvents(events, nb, null, timeout.toMillis());
    }

    public List<EventRepresentation> pollEvents(Events events, int nb, Predicate<EventRepresentation> eventPredicate, Duration timeout) {
        return pollEvents(events, nb, eventPredicate, timeout.toMillis());
    }

    public List<EventRepresentation> pollEvents(Events events, int nb, long timeout) {
        return pollEvents(events, nb, null, timeout);
    }

    public List<EventRepresentation> pollEvents(Events events, int nb, Predicate<EventRepresentation> eventPredicate, long timeout) {
        var limit = System.currentTimeMillis() + timeout;
        var res = new ArrayList<EventRepresentation>();
        do {
            var e = events.poll();
            if (e != null && (eventPredicate==null || eventPredicate.test(e))) {
                res.add(e);
            }
        } while (System.currentTimeMillis() < limit && res.size() < nb);
        return res;
    }

    public List<EventRepresentation> pollEvents(List<Events> eventsSources, int count) {
        return pollEvents(eventsSources, count, null, Duration.ofMillis(0));
    }

    public List<EventRepresentation> pollEvents(List<Events> eventsSources, int count, Predicate<EventRepresentation> eventPredicate) {
        return pollEvents(eventsSources, count, eventPredicate, Duration.ofMillis(0));
    }

    public List<EventRepresentation> pollEvents(List<Events> eventsSources, int count, Duration timeout) {
        return pollEvents(eventsSources, count, null, timeout);
    }

    public List<EventRepresentation> pollEvents(List<Events> eventsSources, int count, Predicate<EventRepresentation> eventPredicate, Duration timeout) {
        var limit = System.currentTimeMillis() + timeout.toMillis();
        var res = new ArrayList<EventRepresentation>();
        do {
            for (var eventsSrc : eventsSources) {
                var newEvents = pollEvents(eventsSrc, count, eventPredicate);
                res.addAll(newEvents);
                count -= newEvents.size();
            }
            if (count > 0) {
                sleep(50);
            }
        }
        while (count > 0 && System.currentTimeMillis() < limit);
        return res;
    }

    public List<AdminEventRepresentation> pollAdminEvents(List<AdminEvents> eventsSources, int count) {
        return pollAdminEvents(eventsSources, count, null, Duration.ofMillis(0));
    }

    public List<AdminEventRepresentation> pollAdminEvents(List<AdminEvents> eventsSources, int count, Predicate<AdminEventRepresentation> eventPredicate) {
        return pollAdminEvents(eventsSources, count, eventPredicate, Duration.ofMillis(0));
    }

    public List<AdminEventRepresentation> pollAdminEvents(List<AdminEvents> eventsSources, int count, Duration timeout) {
        return pollAdminEvents(eventsSources, count, null, timeout);
    }

    public List<AdminEventRepresentation> pollAdminEvents(List<AdminEvents> eventsSources, int count, Predicate<AdminEventRepresentation> eventPredicate, Duration timeout) {
        var limit = System.currentTimeMillis() + timeout.toMillis();
        var res = new ArrayList<AdminEventRepresentation>();
        do {
            for (var eventsSrc : eventsSources) {
                var newEvents = pollEvents(eventsSrc, count, eventPredicate);
                res.addAll(newEvents);
                count -= newEvents.size();
            }
            if (count > 0) {
                sleep(50);
            }
        }
        while (count > 0 && System.currentTimeMillis() < limit);
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
     * @param conditionWhile Condition to wait for. The method will wait until this condition returns false.
     * @param maxDelay       Maximum time to wait in milliseconds. If this time is exceeded, the method will fail.
     * @throws InterruptedException if the thread is interrupted while waiting
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
