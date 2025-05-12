package io.cloudtrust.keycloak.test;

import io.cloudtrust.keycloak.test.container.KeycloakQuarkusConfiguration;
import io.cloudtrust.keycloak.test.container.KeycloakQuarkusContainer;
import io.cloudtrust.keycloak.test.events.EventsManager;
import io.cloudtrust.keycloak.test.util.JwtToolbox;
import org.jboss.logging.Logger;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;

/**
 * This tool is intended to be the single entry point of any access to the Keycloak REST interface
 * If a token has to be revoked, using a KeycloakClientProvider is the safest way of be sure
 * we are using the latest generated token.
 *
 * @author fpe
 */
public class KeycloakClientProvider {
    private static final Logger LOG = Logger.getLogger(KeycloakClientProvider.class);

    private final KeycloakQuarkusConfiguration configuration;
    private final KeycloakQuarkusContainer container;
    private Keycloak adminCli = null;
    private ExtensionApi extensionApi;
    private EventsManager<EventRepresentation> eventsManager;
    private EventsManager<AdminEventRepresentation> adminEventsManager;

    public KeycloakClientProvider(KeycloakQuarkusContainer container, KeycloakQuarkusConfiguration configuration) {
        this.container = container;
        this.configuration = configuration;
    }

    public Keycloak getKeycloakAdminClient() {
        if (this.adminCli == null) {
            String adminCliUrl = this.configuration.getBaseUrl();
            LOG.infof("Creating Keycloak admin client using base URL %s for user %s", adminCliUrl, configuration.getAdminUsername());
            this.adminCli = KeycloakBuilder.builder()
                    .serverUrl(adminCliUrl)
                    .realm(configuration.getAdminRealm())
                    .username(configuration.getAdminUsername())
                    .password(configuration.getAdminSecurity())
                    .clientId(configuration.getAdminClientId())
                    .build();
        }
        return this.adminCli;
    }

    public void reset() {
        if (this.adminCli != null) {
            this.adminCli.close();
            this.adminCli = null;
            this.extensionApi = null;
            this.eventsManager = null;
            this.adminEventsManager = null;
            LOG.debugf("Resetting KC admin client: done");
        }
    }

    public String getTokenPayload() {
        String jwt = this.getKeycloakAdminClient().tokenManager().getAccessTokenString();
        return JwtToolbox.getPayload(jwt, jwt);
    }

    public EventsManager<EventRepresentation> events() {
        if (eventsManager == null) {
            LOG.info("No events manager. Creating one");
            eventsManager = new EventsManager<>(
                    (realmName, configConsumer) -> {
                        LOG.debugf("Activating events (or admin events) configuration from realm %s", realmName);
                        RealmResource realm = getRealm(realmName);
                        RealmEventsConfigRepresentation conf = realm.getRealmEventsConfig();
                        boolean update = false;
                        if (!conf.isEventsEnabled()) {
                            conf.setEventsEnabled(true);
                            update = true;
                        }
                        if (configConsumer != null) {
                            configConsumer.accept(conf);
                        }
                        if (update) {
                            LOG.debugf("Applying events (or admin events) configuration from realm %s", realmName);
                            realm.updateRealmEventsConfig(conf);
                        }
                        LOG.debugf("Events (or admin events) configuration from realm %s is updated", realmName);
                    },
                    r -> this.getRealm(r).clearEvents(),
                    r -> this.getRealm(r).getEvents(),
                    (e1, e2) -> (int) (e1.getTime() - e2.getTime())
            );
            LOG.debug("New events manager created");
        }
        return this.eventsManager;
    }

    public EventsManager<AdminEventRepresentation> adminEvents() {
        if (adminEventsManager == null) {
            LOG.debug("No admin events manager. Creating one");
            adminEventsManager = new EventsManager<>(
                    (realmName, configConsumer) -> {
                        RealmResource realm = getRealm(realmName);
                        RealmEventsConfigRepresentation conf = realm.getRealmEventsConfig();
                        boolean update = false;
                        if (!Boolean.TRUE.equals(conf.isAdminEventsEnabled())) {
                            conf.setAdminEventsEnabled(true);
                            update = true;
                        }
                        if (configConsumer != null) {
                            configConsumer.accept(conf);
                        }
                        if (update) {
                            realm.updateRealmEventsConfig(conf);
                        }
                    },
                    r -> this.getRealm(r).clearAdminEvents(),
                    r -> this.getRealm(r).getAdminEvents(),
                    (e1, e2) -> (int) (e1.getTime() - e2.getTime())
            );
            LOG.debug("New admin events manager created");
        }
        return this.adminEventsManager;
    }

    private RealmResource getRealm(String realmName) {
        return this.getKeycloakAdminClient().realm(realmName);
    }

    public ExtensionApi api() {
        if (extensionApi == null) {
            LOG.infof("Creating new instance of API with base URL %s", this.container.getBaseUrl());
            extensionApi = new ExtensionApi(this.container.getBaseUrl(), this);
        }
        return extensionApi;
    }
}
