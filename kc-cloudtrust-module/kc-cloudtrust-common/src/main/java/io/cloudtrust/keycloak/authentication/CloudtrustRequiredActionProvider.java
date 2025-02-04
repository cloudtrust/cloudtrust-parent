package io.cloudtrust.keycloak.authentication;

import org.keycloak.authentication.RequiredActionProvider;

public interface CloudtrustRequiredActionProvider extends RequiredActionProvider {
    @Override
    default int getMaxAuthAge() {
        // Overriding the max time after a user login to be 24 hours
        return 24 * 60 * 60;
    }
}
