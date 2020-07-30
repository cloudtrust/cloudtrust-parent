package io.cloudtrust.keycloak;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.theme.Theme;
import org.keycloak.theme.ThemeProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

public class ThemeUtils {
    private static final Logger LOG = Logger.getLogger(ThemeUtils.class);

    /**
     * Look for the theme of the current realm
     * @param type Theme type
     * @return A type (if any was found)
     * @throws IOException
     */
    public static Theme findTheme(KeycloakSession session, Theme.Type type) throws IOException {
        RealmModel realm = session.getContext().getRealm();
        String loginTheme = realm.getLoginTheme();
        Set<ThemeProvider> providers = session.getAllProviders(ThemeProvider.class);
        // We iterate through all the theme providers
        for (ThemeProvider provider : providers) {
            // If we've found a provider for this login theme...
            if (provider.hasTheme(loginTheme, type)) {
                // We return the the theme.
                return provider.getTheme(loginTheme, type);
            }
        }
        return null;
    }

    /**
     * Returns a stream to a resource in this realm's theme
     * @param path The path to the resource
     * @return A stream to a resource
     * @throws IOException
     */
    public static InputStream getStreamToResourceImage(KeycloakSession session, String path) throws IOException {
        Theme theme = findTheme(session, Theme.Type.LOGIN);
        if (theme != null) {
            // And return a stream to the resource
            InputStream stream = theme.getResourceAsStream(path);
            if (stream != null) {
                return stream;
            }
        }
        return null;
    }

    /**
     * Load message stored in the theme
     */
    public static String loadMessageFromTheme(KeycloakSession session, UserModel user, String messageId) {
        Theme theme;
        try {
            theme = session.theme().getTheme(Theme.Type.LOGIN);
        } catch (IOException e) {
            LOG.error("Failed to create theme", e);
            return null;
        }

        Locale locale = user!=null ? session.getContext().resolveLocale(user) : Locale.ENGLISH;
        Properties messagesBundle = handleThemeResources(theme, locale);
        return messagesBundle.getProperty(messageId);
    }

    /**
     * Load message bundle (inspired from FreeMarkerLoginFormsProvider).
     */
    public static Properties handleThemeResources(Theme theme, Locale locale) {
        Properties messagesBundle;
        try {
            messagesBundle = theme.getMessages(locale);
        } catch (IOException e) {
            LOG.warn("Failed to load messages", e);
            messagesBundle = new Properties();
        }

        return messagesBundle;
    }
}
