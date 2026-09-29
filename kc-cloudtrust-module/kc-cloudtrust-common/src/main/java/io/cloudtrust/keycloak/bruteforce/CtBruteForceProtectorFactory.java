package io.cloudtrust.keycloak.bruteforce;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.managers.DefaultBruteForceProtectorFactory;

public class CtBruteForceProtectorFactory extends DefaultBruteForceProtectorFactory {
    @Override
    public BruteForceProtector create(KeycloakSession session) {
        return new CtBruteForceProtector(super.create(session));
    }
}
