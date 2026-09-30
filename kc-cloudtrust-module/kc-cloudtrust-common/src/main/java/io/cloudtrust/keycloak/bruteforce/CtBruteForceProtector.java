package io.cloudtrust.keycloak.bruteforce;

import jakarta.ws.rs.core.UriInfo;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.BruteForceProtector;

import java.util.Set;

/**
 * Wraps Keycloak's brute force protector so that it also processes the categories registered in
 * {@link CtBruteForceCategories}. Every call is forwarded to the wrapped protector.
 */
public class CtBruteForceProtector implements BruteForceProtector {
    private final BruteForceProtector delegate;

    public CtBruteForceProtector(BruteForceProtector delegate) {
        this.delegate = delegate;
    }

    @Override
    public void failedLogin(RealmModel realm, UserModel user, ClientConnection clientConnection, UriInfo uriInfo, Set<String> authenticationCategories) {
        delegate.failedLogin(realm, user, clientConnection, uriInfo, CtBruteForceCategories.toProcessedFailureCategories(authenticationCategories));
    }

    @Override
    public void successfulLogin(RealmModel realm, UserModel user, ClientConnection clientConnection, UriInfo uriInfo, Set<String> authenticationCategories) {
        delegate.successfulLogin(realm, user, clientConnection, uriInfo, CtBruteForceCategories.toProcessedSuccessCategories(authenticationCategories));
    }

    @Override
    public boolean isTemporarilyDisabled(KeycloakSession session, RealmModel realm, UserModel user) {
        return delegate.isTemporarilyDisabled(session, realm, user);
    }

    @Override
    public boolean isPermanentlyLockedOut(KeycloakSession session, RealmModel realm, UserModel user) {
        return delegate.isPermanentlyLockedOut(session, realm, user);
    }

    @Override
    public void cleanUpPermanentLockout(KeycloakSession session, RealmModel realm, UserModel user) {
        delegate.cleanUpPermanentLockout(session, realm, user);
    }

    @Override
    public void close() {
        delegate.close();
    }
}
