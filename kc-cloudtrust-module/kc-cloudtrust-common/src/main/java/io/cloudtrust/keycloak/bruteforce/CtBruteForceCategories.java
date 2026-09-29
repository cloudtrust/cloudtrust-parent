package io.cloudtrust.keycloak.bruteforce;

import org.keycloak.models.credential.PasswordCredentialModel;
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
    /**
     * Allowed category forwarded to Keycloak for the CloudTrust categories. Keycloak only uses the categories of a
     * processed login to detect OTP (which also counts/clears secondary authentication failures), so the password
     * category gives the CloudTrust categories the same processing they had up to Keycloak 26.6.4: primary failures
     * are counted on failure and cleared on success.
     */
    public static final Set<String> PRIMARY_CATEGORIES = Set.of(PasswordCredentialModel.TYPE);

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
     * Maps the authentication categories of a login failure to the ones Keycloak's brute force protector has to process.
     * <p>
     * When none of the categories is allowed by Keycloak but at least one is registered by CloudTrust, the categories
     * are replaced by {@link #PRIMARY_CATEGORIES}. In every other case, including null (which Keycloak still counts
     * as a failure), the categories are left untouched.
     *
     * @param categories categories received by the brute force protector
     * @return categories to forward to Keycloak's brute force protector
     */
    public static Set<String> toProcessedFailureCategories(Set<String> categories) {
        if (categories != null
                && Collections.disjoint(DefaultBruteForceProtector.ALLOWED_AUTHENTICATION_CATEGORIES, categories)
                && !Collections.disjoint(REGISTERED, categories)) {
            return PRIMARY_CATEGORIES;
        }
        return categories;
    }

    /**
     * Maps the authentication categories of a successful login to the ones Keycloak's brute force protector has to
     * process.
     * <p>
     * Same as {@link #toProcessedFailureCategories(Set)}, except that null is also replaced by
     * {@link #PRIMARY_CATEGORIES}: up to Keycloak 26.6.4 a successful login without category reset the failure count,
     * since Keycloak 26.6.7 DefaultBruteForceProtector#successfulLogin silently ignores it (failedLogin still counts
     * a null category). CloudTrust services that report a success with a null category rely on that reset.
     *
     * @param categories categories received by the brute force protector
     * @return categories to forward to Keycloak's brute force protector
     */
    public static Set<String> toProcessedSuccessCategories(Set<String> categories) {
        return categories == null ? PRIMARY_CATEGORIES : toProcessedFailureCategories(categories);
    }

    static void clear() {
        REGISTERED.clear();
    }
}
