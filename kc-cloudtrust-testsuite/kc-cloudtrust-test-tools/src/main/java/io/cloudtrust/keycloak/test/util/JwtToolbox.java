package io.cloudtrust.keycloak.test.util;

import java.util.Base64;

public interface JwtToolbox {
    public static String getPayload(String jwt) {
        String[] tokenParts = jwt.split("\\.");
        try {
            return new String(Base64.getDecoder().decode(tokenParts[1]));
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }

    public static String getPayload(String jwt, String defaultValue) {
        String res = getPayload(jwt);
        return res != null ? res : defaultValue;
    }
}
