package io.cloudtrust.keycloak.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cloudtrust.keycloak.test.container.KeycloakDeploy;
import io.cloudtrust.keycloak.test.init.InjectionException;
import io.cloudtrust.keycloak.test.init.TestInitializer;
import io.cloudtrust.keycloak.test.matchers.EventMatcher;
import io.cloudtrust.keycloak.test.util.FlowUtil;
import io.cloudtrust.keycloak.test.util.OidcTokenProvider;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;

public abstract class AbstractInKeycloakTest {
    private static final Logger LOG = Logger.getLogger(AbstractInKeycloakTest.class);
    public static final int LISTEN_PORT = 9995;

    private static Undertow httpServer;
    protected ObjectMapper mapper = new ObjectMapper();
    private String token;
    private String defaultRealmName = "test";
    private Queue<EventRepresentation> events = new LinkedList<>();
    private Queue<AdminEventRepresentation> adminEvents = new LinkedList<>();
    private Set<String> realmWithActivatedEvents = new HashSet<>();
    private int readEvents = 0;
    private int readAdminEvents = 0;

    public interface ConsumerExcept<T, E extends Throwable> {
        void accept(T param) throws E;
    }

	private TestInitializer testInitializer;

    protected static void startHttpServer(HttpHandler handler) {
    	startHttpServer(handler, LISTEN_PORT);
    }

    protected static void startHttpServer(HttpHandler handler, int port) {
		stopHttpServer();

		Undertow server = Undertow.builder()
                .addHttpListener(port, "0.0.0.0")
                .setHandler(handler)
                .build();
        server.start();
        AbstractInKeycloakTest.httpServer = server;
        Runtime.getRuntime().addShutdownHook(new Thread(AbstractInKeycloakTest::stopHttpServer));
    }

    protected static synchronized void stopHttpServer() {
    	if (AbstractInKeycloakTest.httpServer!=null) {
    		AbstractInKeycloakTest.httpServer.stop();
    		AbstractInKeycloakTest.httpServer = null;
    	}
    }

    public String getKeycloakURL() {
    	return KeycloakDeploy.getContainer().getBaseUrl();
    }

    public void createRealm(String filename) throws IOException {
        createRealm(null, filename);
    }

    public void createRealm(String realmName, String filename) throws IOException {
        createRealm(realmName, filename, r -> {
        });
    }

    public void createRealm(String realmName, String filename, ConsumerExcept<RealmResource, IOException> whenCreated) throws IOException {
        Keycloak kc = KeycloakDeploy.getContainer().getKeycloakAdminClient();
        RealmResource testRealm = importTestRealm(kc, realmName, filename);
        whenCreated.accept(testRealm);
    }

	protected RealmResource importTestRealm(Keycloak keycloak, String realmName, String realmFilePath) throws IOException {
        RealmRepresentation realmRepresentation = new ObjectMapper().readValue(
                getClass().getResourceAsStream(realmFilePath), RealmRepresentation.class);
        if (realmName==null) {
        	realmName = realmRepresentation.getRealm();
        }
		this.realmWithActivatedEvents.remove(realmName);
        RealmResource realm = keycloak.realm(realmName);
        try {
            realm.remove();
        } catch (NotFoundException nfe) {
        	// Ignore
        }
        keycloak.realms().create(realmRepresentation);
        defaultRealmName = realmName;
        return keycloak.realm(realmName);
    }

    public RealmResource getRealm() {
    	return getRealm(defaultRealmName);
    }

    public RealmResource getRealm(String realmName) {
        return KeycloakDeploy.getContainer().getKeycloakAdminClient().realm(realmName);
    }

    public void updateRealm(String realmName, Consumer<RealmRepresentation> updater) {
    	RealmResource realmResource = this.getRealm(realmName);
    	RealmRepresentation realm = realmResource.toRepresentation();
    	updater.accept(realm);
    	realmResource.update(realm);
    }

    public List<UserRepresentation> searchUsers(String username) {
    	return searchUsers(defaultRealmName, username);
    }

    public List<UserRepresentation> searchUsers(String realmName, String username) {
		return getRealm(realmName).users().search(username);
	}

    public Map<String, List<String>> getUserAttributes(String username) {
    	return getUserAttributes(defaultRealmName, username);
    }

    public Map<String, List<String>> getUserAttributes(String realmName, String username) {
		return searchUsers(realmName, username).get(0).getAttributes();
	}

    public List<String> getUserAttribute(String username, String attributeName) {
    	return getUserAttribute(username, attributeName);
    }

    public List<String> getUserAttribute(String realmName, String username, String attributeName) {
    	Map<String, List<String>> attrbs = getUserAttributes(realmName, username);
    	List<String> res = attrbs==null ? null : attrbs.get(attributeName);
		return res==null ? Collections.emptyList() : res;
    }

    public String getUserAttributeAsString(String username, String attributeName) {
    	return getUserAttributeAsString(defaultRealmName, username, attributeName);
    }

    public String getUserAttributeAsString(String realmName, String username, String attributeName) {
    	List<String> attrbs = getUserAttribute(realmName, username, attributeName);
    	return attrbs==null || attrbs.isEmpty() ? null : attrbs.get(0);
    }

    public int getUserAttributeAsInt(String username, String attributeName) {
    	return getUserAttributeAsInt(defaultRealmName, username, attributeName);
    }

    public int getUserAttributeAsInt(String realmName, String username, String attributeName) {
    	try {
    		return Integer.parseInt(getUserAttributeAsString(realmName, username, attributeName));
    	} catch (Exception e) {
    		return 0;
    	}
    }

    public void setUserAttribute(String username, String attributeName, List<String> values) {
    	setUserAttribute(defaultRealmName, username, attributeName, values);
    }

    public void setUserAttribute(String realmName, String username, String attributeName, List<String> values) {
    	RealmResource testRealm = getRealm(realmName);
        String userId = testRealm.users().search(username).get(0).getId();
        UserResource userRes = testRealm.users().get(userId);
        UserRepresentation user = userRes.toRepresentation();
        user.getAttributes().put(attributeName, values);
        userRes.update(user);
    }

    public void removeUserAttribute(String username, String attributeName) {
    	removeUserAttribute(defaultRealmName, username, attributeName);
    }

    public void removeUserAttribute(String realmName, String username, String attributeName) {
    	RealmResource testRealm = getRealm(realmName);
        // remove mobile phone from user
        String userId = testRealm.users().search(username).get(0).getId();
        UserResource userRes = testRealm.users().get(userId);
        UserRepresentation user = userRes.toRepresentation();
        user.getAttributes().remove(attributeName);
        userRes.update(user);
    }

    public Stream<CredentialRepresentation> getUserCredentials(String username, Predicate<? super CredentialRepresentation> predicate) {
    	return getUserCredentials(defaultRealmName, username, predicate);
    }

    public Stream<CredentialRepresentation> getUserCredentials(String realmName, String username, Predicate<? super CredentialRepresentation> predicate) {
    	RealmResource testRealm = getRealm(realmName);
        return testRealm.users().get(testRealm.users().search(username).get(0).getId()).credentials().stream()
                .filter(predicate);
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
    protected void createUser(String username, Consumer<UserRepresentation> userUpdater) {
    	this.createUser(this.getRealm(), username, userUpdater);
    }

    protected void createUser(RealmResource realm, String username, Consumer<UserRepresentation> userUpdater) {
        CredentialRepresentation credentialPass = new CredentialRepresentation();
        credentialPass.setType(CredentialRepresentation.PASSWORD);
        credentialPass.setValue("password+");

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
        }
    }

    protected Keycloak getKeycloakAdminClient() {
        return KeycloakDeploy.getContainer().getKeycloakAdminClient();
    }

    protected FlowUtil getFlowUtil() {
    	return getFlowUtil(defaultRealmName);
    }

    protected FlowUtil getFlowUtil(String realmName) {
    	return FlowUtil.inCurrentRealm(getRealm(realmName));
    }

    public ClientResource findClientResourceById(RealmResource realm, String id) {
        for (ClientRepresentation c : realm.clients().findAll()) {
            if (c.getId().equals(id)) {
                return realm.clients().get(c.getId());
            }
        }
        return null;
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

    protected void sleep(Duration maxDuration, long interval, BooleanSupplier shouldStop) {
    	this.sleep(maxDuration.toMillis(), interval, shouldStop);
    }

    protected void sleep(Duration maxDuration, BooleanSupplier shouldStop) {
    	sleep(maxDuration.toMillis(), 100, shouldStop);
    }

    protected void sleep(long maxDuration, BooleanSupplier shouldStop) {
    	sleep(maxDuration, 100, shouldStop);
    }

    protected void sleep(long maxDuration, long interval, BooleanSupplier shouldStop) {
        try {
            long limit = System.currentTimeMillis() + maxDuration;
            while (System.currentTimeMillis() < limit) {
                long maxPause = Math.max(0, limit - System.currentTimeMillis());
                Thread.sleep(Math.min(interval, maxPause));
                maxDuration -= interval;
                if (shouldStop.getAsBoolean()) {
                    break;
                }
            }
        } catch (InterruptedException ie) {
            // Ignore exception
        }
    }

    /**
     * Event management: activate events
     */
    public void activateEvents() {
    	activateEvents(defaultRealmName, null, false);
    }

    public void activateEvents(String realmName, Consumer<RealmEventsConfigRepresentation> configConsumer, boolean adminEventsToo) {
    	RealmResource realm = getRealm(realmName);
    	RealmEventsConfigRepresentation conf = realm.getRealmEventsConfig();
    	boolean update = false;
    	if (!conf.isEventsEnabled()) {
        	conf.setEventsEnabled(true);
        	realmWithActivatedEvents.add(realmName);
    		update = true;
    	}
    	if (adminEventsToo && !Boolean.TRUE.equals(conf.isAdminEventsEnabled())) {
        	conf.setAdminEventsEnabled(true);
        	update = true;
    	}
    	if (configConsumer!=null) {
    		configConsumer.accept(conf);
    	}
    	if (update) {
        	realm.updateRealmEventsConfig(conf);
    	}
    }

    /**
     * Event management: clear events
     */
    public void clearEvents() {
    	this.realmWithActivatedEvents.stream().map(this::getRealm).forEach(realm -> {
        	realm.clearEvents();
        	realm.clearAdminEvents();
    	});
    	readEvents = 0;
    	readAdminEvents = 0;
    }

    /**
     * Event management: poll event
     */
    protected EventRepresentation pollEvent() {
    	if (events.isEmpty()) {
    		List<EventRepresentation> newEvents = new ArrayList<>();
    		for(String realmName : this.realmWithActivatedEvents) {
    			List<EventRepresentation> realmEvents = this.getRealm(realmName).getEvents();
    			if (realmEvents!=null) {
    				newEvents.addAll(realmEvents);
    			}
    		}
    		if (!newEvents.isEmpty()) {
    			newEvents.sort((o1, o2) -> (int)(o1.getTime()-o2.getTime()));
        		events.clear();
        		events.addAll(newEvents);
        		for(int i=0; i<readEvents; i++) {
        			events.poll();
        		}
    		}
    	}
    	EventRepresentation res = events.poll();
    	if (res!=null) {
    		readEvents++;
    		this.logObject("** FPX ** Event", res);
    	}
    	return res;
    }

    protected AdminEventRepresentation pollAdminEvent() {
    	if (adminEvents.isEmpty()) {
    		List<AdminEventRepresentation> newEvents = new ArrayList<>();
    		for(String realmName : this.realmWithActivatedEvents) {
    			List<AdminEventRepresentation> realmEvents = this.getRealm(realmName).getAdminEvents();
    			if (realmEvents!=null) {
    				newEvents.addAll(realmEvents);
    			}
    		}
    		if (!newEvents.isEmpty()) {
    			newEvents.sort((o1, o2) -> (int)(o1.getTime()-o2.getTime()));
        		adminEvents.clear();
        		adminEvents.addAll(newEvents);
        		for(int i=0; i<readAdminEvents; i++) {
        			events.poll();
        		}
    		}
    	}
    	AdminEventRepresentation res = adminEvents.poll();
    	if (res!=null) {
    		readAdminEvents++;
    		this.logObject("** FPX ** AdminEvent", res);
    	}
    	return res;
    }

    protected void assertHasNoEvent() {
        assertThat(pollEvent(), EventMatcher.doesNotExist());
    }

    /**
     * Use assertThat(pollEvent(), EventMatcher.exists()) instead
     * @param consumer
     *
     * @deprecated
     */
    @Deprecated
    protected void assertHasEvent(Consumer<EventRepresentation> consumer) {
        EventRepresentation event = pollEvent();
        assertThat(event, notNullValue());
        consumer.accept(event);
    }

    /**
     * API management
     */
    protected void initializeToken() {
    	String accessToken = this.getKeycloakAdminClient().tokenManager().getAccessTokenString();
    	this.setToken(accessToken);
    }

    protected String getToken() {
    	assertThat(this.token, is(notNullValue()));
    	return this.token;
    }

    protected void setToken(String accessToken) {
		token = accessToken;
    }

    protected <T> T queryApi(Class<T> clazz, String method, String apiPath) throws IOException, URISyntaxException {
        return mapper.readValue(callApi(method, apiPath, new ArrayList<>()), clazz);
    }

    protected <T> T queryApi(Class<T> clazz, String method, String apiPath, List<NameValuePair> nvps) throws IOException, URISyntaxException {
        return mapper.readValue(callApi(method, apiPath, nvps), clazz);
    }

    protected <T> T queryApi(TypeReference<T> typeRef, String method, String apiPath, List<NameValuePair> params) throws IOException, URISyntaxException {
        return mapper.readValue(callApi(method, apiPath, params), typeRef);
    }

    protected String callApi(String apiPath) throws IOException, URISyntaxException {
        return callApi("GET", apiPath, new ArrayList<>());
    }

    protected String callApi(String method, String apiPath) throws IOException, URISyntaxException {
        return callApi(method, apiPath, new ArrayList<>());
    }

    protected String callApi(String method, String apiPath, List<NameValuePair> nvps) throws IOException, URISyntaxException {
        return callApi(method, apiPath, nvps, null);
    }

    protected String callApiJSON(String method, String apiPath, Object jsonable) throws IOException, URISyntaxException {
        return callApiJSON(method, apiPath, new ArrayList<>(), jsonable);
    }

    protected String callApiJSON(String method, String apiPath, List<NameValuePair> nvps, Object jsonable) throws IOException, URISyntaxException {
        String json = new ObjectMapper().writeValueAsString(jsonable);
        StringEntity requestEntity = new StringEntity(json, ContentType.APPLICATION_JSON);
        return callApi(method, apiPath, nvps, requestEntity);
    }

    protected String callApi(String method, String apiPath, List<NameValuePair> nvps, HttpEntity entity) throws IOException, URISyntaxException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
        	String uri = this.getKeycloakURL() + apiPath;
            URIBuilder uriBuilder = new URIBuilder(uri);
            uriBuilder.addParameters(nvps);
            HttpRequestBase get = createHttpRequest(method, uriBuilder.build(), entity);
            get.addHeader("Authorization", "Bearer " + token);
            LOG.infof("** FPX ** Authorization: Bearer %s");

            HttpResponse response = client.execute(get);
            if (response.getStatusLine().getStatusCode() / 100 != 2) {
                throw new HttpResponseException(response.getStatusLine().getStatusCode(), "call to "+uri+" failed: " + response.getStatusLine().getStatusCode());
            }
            if (response.getEntity() != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
                    return reader.lines().collect(Collectors.joining());
                }
            }
            return null;
        }
    }

    private HttpRequestBase createHttpRequest(String method, URI uri, HttpEntity entity) throws HttpResponseException {
        switch (method) {
            case "GET":
                return new HttpGet(uri);
            case "PUT":
                return addBodyToHttpRequest(new HttpPut(uri), entity);
            default:
                throw new HttpResponseException(405, "Unsupported method " + method);
        }
    }

    private HttpRequestBase addBodyToHttpRequest(HttpEntityEnclosingRequestBase httpRequest, HttpEntity entity) {
        if (entity != null) {
            httpRequest.setEntity(entity);
        }
        return httpRequest;
    }

    public OidcTokenProvider createOidcTokenProvider() {
        return createOidcTokenProvider("VPN", "VPN-CLIENT-SECRET");
    }

    public OidcTokenProvider createOidcTokenProvider(String username, String password) {
        return new OidcTokenProvider(getKeycloakURL(), "/realms/test/protocol/openid-connect/token", username, password);
    }

    public void injectComponents() throws InjectionException {
    	injectComponents(false);
    }

    public void injectComponents(boolean forceReInit) throws InjectionException {
    	if (this.testInitializer==null) {
    		this.testInitializer = new TestInitializer();
    	}
    	testInitializer.init(this, forceReInit);
    }

    public void logObject(String message, Object obj) {
		try {
			String value = obj==null ? "(null)" : this.mapper.writeValueAsString(obj);
			LOG.info(message+": "+value);
		} catch (JsonProcessingException e) {
			LOG.info(message+": can't log object");
		}
    }
}