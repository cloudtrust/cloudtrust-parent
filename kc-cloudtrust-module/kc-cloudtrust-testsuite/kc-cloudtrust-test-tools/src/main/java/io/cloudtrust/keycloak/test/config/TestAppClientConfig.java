package io.cloudtrust.keycloak.test.config;

import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientBuilder;

public class TestAppClientConfig implements ClientConfig {
    @Override
    public ClientBuilder configure(ClientBuilder client) {
        return client.clientId("test-app").redirectUris("http://localhost:8080/*");
    }
}