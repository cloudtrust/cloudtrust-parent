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
    void nullCategoriesAreKeptTest() {
        Assertions.assertNull(CtBruteForceCategories.toProcessedCategories(null));
    }

    @Test
    void registeredCategoryIsProcessedTest() {
        Assertions.assertNull(CtBruteForceCategories.toProcessedCategories(Set.of("ctsms")));
    }

    @Test
    void allowedCategoryIsKeptTest() {
        Set<String> categories = Set.of("otp");
        Assertions.assertSame(categories, CtBruteForceCategories.toProcessedCategories(categories));
        categories = Set.of("otp", "ctsms");
        Assertions.assertSame(categories, CtBruteForceCategories.toProcessedCategories(categories));
    }

    @Test
    void unknownCategoryIsKeptTest() {
        Set<String> categories = Set.of("webauthn");
        Assertions.assertSame(categories, CtBruteForceCategories.toProcessedCategories(categories));
        Assertions.assertTrue(CtBruteForceCategories.toProcessedCategories(Set.of()).isEmpty());
    }
}
