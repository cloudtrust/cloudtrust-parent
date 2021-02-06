package io.cloudtrust.keycloak.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudtrust.keycloak.test.util.OidcTokenProvider;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.test.FluentTestsHelper;
import org.keycloak.testsuite.util.OAuthClient;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public abstract class AuthenticatorTest {
    protected static final String KEYCLOAK_URL = getKeycloakUrl();

    protected static Undertow server;

    @Drone
    protected WebDriver driver;

    @ArquillianResource
    protected OAuthClient oauth;

    public static final int LISTEN_PORT = 9995;
    protected RealmResource testRealm;
    protected Keycloak keycloak;
    protected String token;

    protected static ThreadLocal<ObjectMapper> objectMapperProvider = ThreadLocal.withInitial(ObjectMapper::new);

    protected void setupTestRealm() throws IOException {
    }

    protected static String getKeycloakUrl() {
        String url = FluentTestsHelper.DEFAULT_KEYCLOAK_URL;
        try {
            URI uri = new URI(FluentTestsHelper.DEFAULT_KEYCLOAK_URL);
            url = url.replace(String.valueOf(uri.getPort()), System.getProperty("auth.server.http.port", "8080"));
        } catch (Exception e) {
            // Ignore
        }
        return url;
    }

    public static WebArchive createWebArchive() {
        return ShrinkWrap.create(WebArchive.class, "run-on-server-classes.war")
                .addPackages(true, "io.cloudtrust.keycloak")
                .addAsManifestResource(new File("src/test/resources", "manifest.xml"));
    }

    @Before
    public void createTestRealm() throws IOException {
        oauth.init(driver);
        keycloak = Keycloak.getInstance(KEYCLOAK_URL, "master", "admin", "admin", "admin-cli");
        token = keycloak.tokenManager().getAccessTokenString();
        testRealm = importTestRealm(keycloak, "test", "/testrealm.json");
        setupTestRealm();
    }

    @After
    public void deleteTestRealm() {
        testRealm.remove();
    }

    protected static Undertow startHttpServer(HttpHandler handler) {
        Undertow server = Undertow.builder()
                .addHttpListener(LISTEN_PORT, "0.0.0.0", handler)
                .build();
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        return server;
    }

    public OidcTokenProvider createOidcTokenProvider() {
        return createOidcTokenProvider("VPN", "VPN-CLIENT-SECRET");
    }

    public OidcTokenProvider createOidcTokenProvider(String username, String password) {
        return new OidcTokenProvider(KEYCLOAK_URL, "/realms/test/protocol/openid-connect/token", username, password);
    }

    /**
     * Creates an enabled user with email=username+"@test.com" and password="password+"
     *
     * @param username    Username
     * @param userUpdater Lambda used to customize the user before it is created
     */
    public void createUser(String username, Consumer<UserRepresentation> userUpdater) {
        CredentialRepresentation credentialPass = new CredentialRepresentation();
        credentialPass.setType(CredentialRepresentation.PASSWORD);
        credentialPass.setValue("password+");

        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setEnabled(true);
        user.setCredentials(new LinkedList<>());
        user.getCredentials().add(credentialPass);
        user.setAttributes(new HashMap<String, List<String>>());
        userUpdater.accept(user);
        try (Response createUser = testRealm.users().create(user)) {
            Assert.assertEquals(201, createUser.getStatus());
        }
    }

    protected RealmResource importTestRealm(Keycloak keycloak, String realmName, String realmFilePath) throws IOException {
        RealmRepresentation realmRepresentation = objectMapperProvider.get().readValue(
                getClass().getResourceAsStream(realmFilePath), RealmRepresentation.class);
        keycloak.realms().create(realmRepresentation);
        return keycloak.realm(realmName);
    }
}
