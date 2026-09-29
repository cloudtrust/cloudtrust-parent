package io.cloudtrust.keycloak.bruteforce;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

class CtBruteForceCategoriesTest {
    @BeforeEach
    void setup() {
        CtBruteForceCategories.clear();
        CtBruteForceCategories.register("ctsms");
        CtBruteForceCategories.register(null);
    }

    @AfterEach
    void cleanup() {
        CtBruteForceCategories.clear();
    }

    @Test
    void registerTest() {
        Assertions.assertEquals(Set.of("ctsms"), CtBruteForceCategories.getRegistered());
    }

    @Test
    void nullFailureCategoriesAreKeptTest() {
        Assertions.assertNull(CtBruteForceCategories.toProcessedFailureCategories(null));
    }

    @Test
    void nullSuccessCategoriesAreProcessedTest() {
        Assertions.assertEquals(Set.of("password"), CtBruteForceCategories.toProcessedSuccessCategories(null));
    }

    @Test
    void registeredCategoryIsProcessedTest() {
        Assertions.assertEquals(Set.of("password"), CtBruteForceCategories.toProcessedFailureCategories(Set.of("ctsms")));
        Assertions.assertEquals(Set.of("password"), CtBruteForceCategories.toProcessedSuccessCategories(Set.of("ctsms")));
    }

    @Test
    void allowedCategoryIsKeptTest() {
        Set<String> categories = Set.of("otp");
        Assertions.assertSame(categories, CtBruteForceCategories.toProcessedFailureCategories(categories));
        Assertions.assertSame(categories, CtBruteForceCategories.toProcessedSuccessCategories(categories));
        categories = Set.of("otp", "ctsms");
        Assertions.assertSame(categories, CtBruteForceCategories.toProcessedFailureCategories(categories));
        Assertions.assertSame(categories, CtBruteForceCategories.toProcessedSuccessCategories(categories));
    }

    @Test
    void unknownCategoryIsKeptTest() {
        Set<String> categories = Set.of("webauthn");
        Assertions.assertSame(categories, CtBruteForceCategories.toProcessedFailureCategories(categories));
        Assertions.assertSame(categories, CtBruteForceCategories.toProcessedSuccessCategories(categories));
        Assertions.assertTrue(CtBruteForceCategories.toProcessedFailureCategories(Set.of()).isEmpty());
    }
}
