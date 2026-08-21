package io.cloudtrust.keycloak.authentication;

import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;

/**
 * Overrides the max time after a user login during which a required action can still be triggered without a fresh
 * re-authentication, unconditionally setting it to 24 hours instead of the Keycloak default of 5 minutes.
 */
public interface CloudtrustRequiredActionProvider extends RequiredActionProvider {
    @Override
    default int getMaxAuthAge(KeycloakSession session) {
        // Overriding the max time after a user login to be 24 hours
        return 24 * 60 * 60;
    }
}
