package io.cloudtrust.keycloak;

import org.keycloak.Config;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.function.Consumer;

public class ConfigUtils {
    private ConfigUtils() {
    }

    /**
     * Do not use in production environment - DEV ONLY
     **/
    public static void showConfig(Config.Scope config, Consumer<String> printer) {
        printer.accept("Config.Scope: class<" + config.getClass().getName() + ">");
        try {
            String[] scope = (String[]) getDeclaredField(config, "scope");
            printer.accept("Config.Scope: Scope: " + Arrays.toString(scope));

            String prefix = (String) getDeclaredField(config, "prefix");
            printer.accept("Config.Scope: Prefix: " + prefix);
            printer.accept("Config.Scope: Suggested prefix: " + toDashCase(prefix));
            for (String key : config.getPropertyNames()) {
                key = key.replace(prefix, "").substring(1);
                printer.accept("Config.Scope: " + key + ": " + config.get(key));
            }
        } catch (Exception e) {
            printer.accept("Config.Scope: failed. " + e.getMessage());
        }
    }

    private static String toDashCase(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        boolean l = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (l && Character.isUpperCase(c)) {
                sb.append('-');
                c = Character.toLowerCase(c);
                l = false;
            } else {
                l = Character.isLowerCase(c);
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static Object getDeclaredField(Config.Scope config, String declaredField) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Class<?> clazz = config.getClass();
        Field scopeField = clazz.getDeclaredField(declaredField);
        scopeField.setAccessible(true);
        return scopeField.get(config);
    }
}
