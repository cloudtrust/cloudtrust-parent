package io.cloudtrust.keycloak.bruteforce;

import org.keycloak.services.managers.DefaultBruteForceProtector;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authentication categories (credential types) that CloudTrust providers want to be covered by brute force protection.
 * <p>
 * Since Keycloak 26.6, DefaultBruteForceProtector silently ignores login failures and successes whose authentication
 * categories are all outside DefaultBruteForceProtector.ALLOWED_AUTHENTICATION_CATEGORIES (password, otp,
 * recovery-authn-codes). Up to Keycloak 26.6.4 CloudTrust providers added their own credential types to that list in
 * postInit(); since Keycloak 26.6.7 the list is an immutable Set and adding to it throws UnsupportedOperationException.
 * Providers now register their categories here instead, and {@link CtBruteForceProtectorFactory} makes the default
 * brute force protector count them again.
 */
public final class CtBruteForceCategories {
    private static final Set<String> REGISTERED = ConcurrentHashMap.newKeySet();

    private CtBruteForceCategories() {
    }

    /**
     * Registers an authentication category (usually a credential type or an authenticator reference category) so that
     * login failures and successes of this category are processed by the brute force protector.
     *
     * @param category the category to register, ignored if null
     */
    public static void register(String category) {
        if (category != null) {
            REGISTERED.add(category);
        }
    }

    public static Set<String> getRegistered() {
        return Collections.unmodifiableSet(REGISTERED);
    }

    /**
     * Maps the authentication categories received by the brute force protector to the ones it has to process.
     * <p>
     * When none of the categories is allowed by Keycloak but at least one is registered by CloudTrust, the categories
     * are replaced by null: Keycloak processes a null category set like any allowed non-OTP category, which is what it
     * did for the CloudTrust categories up to Keycloak 26.6.4. In every other case the categories are left untouched.
     *
     * @param categories categories received by the brute force protector
     * @return categories to forward to Keycloak's brute force protector
     */
    public static Set<String> toProcessedCategories(Set<String> categories) {
        if (categories != null
                && Collections.disjoint(DefaultBruteForceProtector.ALLOWED_AUTHENTICATION_CATEGORIES, categories)
                && !Collections.disjoint(REGISTERED, categories)) {
            return null;
        }
        return categories;
    }

    static void clear() {
        REGISTERED.clear();
    }
}
