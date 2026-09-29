package io.cloudtrust.keycloak.bruteforce;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.services.managers.BruteForceProtector;
import org.mockito.Mockito;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

class CtBruteForceProtectorTest {
    private BruteForceProtector delegate;
    private CtBruteForceProtector protector;

    @BeforeEach
    void setup() {
        CtBruteForceCategories.clear();
        CtBruteForceCategories.register("ctsms");
        delegate = Mockito.mock(BruteForceProtector.class);
        protector = new CtBruteForceProtector(delegate);
    }

    @AfterEach
    void cleanup() {
        CtBruteForceCategories.clear();
    }

    @Test
    void registeredCategoryIsForwardedAsPrimaryTest() {
        protector.failedLogin(null, null, null, null, Set.of("ctsms"));
        verify(delegate).failedLogin(any(), any(), any(), any(), eq(Set.of("password")));
        protector.successfulLogin(null, null, null, null, Set.of("ctsms"));
        verify(delegate).successfulLogin(any(), any(), any(), any(), eq(Set.of("password")));
    }

    @Test
    void nullCategoryTest() {
        protector.failedLogin(null, null, null, null, null);
        verify(delegate).failedLogin(any(), any(), any(), any(), isNull());
        protector.successfulLogin(null, null, null, null, null);
        verify(delegate).successfulLogin(any(), any(), any(), any(), eq(Set.of("password")));
    }

    @Test
    void otherCategoryIsForwardedUnchangedTest() {
        protector.failedLogin(null, null, null, null, Set.of("password"));
        verify(delegate).failedLogin(any(), any(), any(), any(), eq(Set.of("password")));
        protector.successfulLogin(null, null, null, null, Set.of("webauthn"));
        verify(delegate).successfulLogin(any(), any(), any(), any(), eq(Set.of("webauthn")));
    }

    @Test
    void otherMethodsAreDelegatedTest() {
        protector.isTemporarilyDisabled(null, null, null);
        verify(delegate).isTemporarilyDisabled(null, null, null);
        protector.isPermanentlyLockedOut(null, null, null);
        verify(delegate).isPermanentlyLockedOut(null, null, null);
        protector.cleanUpPermanentLockout(null, null, null);
        verify(delegate).cleanUpPermanentLockout(null, null, null);
        protector.close();
        verify(delegate).close();
    }
}
