package io.cloudtrust.keycloak.authentication;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.models.KeycloakSession;
import org.mockito.Mockito;

class CloudtrustRequiredActionProviderTest {
    @Test
    void getMaxAuthAgeIsAlwaysOneDay() {
        CloudtrustRequiredActionProvider provider = new CloudtrustRequiredActionProvider() {
            @Override
            public void evaluateTriggers(RequiredActionContext context) {
                // not under test
            }

            @Override
            public void requiredActionChallenge(RequiredActionContext context) {
                // not under test
            }

            @Override
            public void processAction(RequiredActionContext context) {
                // not under test
            }

            @Override
            public void close() {
                // not under test
            }
        };

        KeycloakSession session = Mockito.mock(KeycloakSession.class);
        Assertions.assertEquals(24 * 60 * 60, provider.getMaxAuthAge(session));
        Assertions.assertEquals(24 * 60 * 60, provider.getMaxAuthAge(null));
    }
}
