package io.cloudtrust.keycloak.test.container;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class KeycloakDeploy implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {
    private static final Logger LOG = Logger.getLogger(KeycloakDeploy.class);
    private static KeycloakDeploy singleton = null;
    private final KeycloakQuarkusConfiguration kcConfig;
    private KeycloakQuarkusContainer keycloak;

    public KeycloakDeploy() {
        KeycloakDeploy.declareSingleton(this);
        kcConfig = ConfigurationFactory.get().getConfiguration("./keycloak.properties");
    }

    public static KeycloakDeploy get() {
        return KeycloakDeploy.singleton;
    }

    public static KeycloakQuarkusContainer getContainer() {
        return KeycloakDeploy.singleton.keycloak;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        keycloak = KeycloakQuarkusContainer.start(kcConfig);
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopKeycloak));
        LOG.infof("Started Keycloak on %s", kcConfig.getBaseUrl());
    }

    private void stopKeycloak() {
        if (this.keycloak != null) {
            synchronized (kcConfig) {
                if (this.keycloak != null) {
                    this.keycloak.stop();
                    this.keycloak = null;
                }
            }
        }
    }

    @Override
    public void close() {
        stopKeycloak();
    }

    private static void declareSingleton(KeycloakDeploy value) {
        KeycloakDeploy.singleton = value;
    }
}
