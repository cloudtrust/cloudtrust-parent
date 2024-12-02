package io.cloudtrust.keycloak.test.dbprovider;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class DatabaseRealmResourceProviderFactory implements RealmResourceProviderFactory {
    public static final String ID = "database-provider";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new DatabaseRealmResourceProvider(session);
	}

    @Override
    public void init(Scope config) {
        // Nothing to do
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Nothing to do
    }

    @Override
    public void close() {
        // Nothing to do
    }
}
