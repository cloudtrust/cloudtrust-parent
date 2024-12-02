package io.cloudtrust.crypto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudtrust.exception.CloudtrustRuntimeException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for the module
 */
public class CryptoUtil {
    private static final String DB_HMAC_KEY_ENV_VAR_NAME = "DB_HMAC_KEY";
    private static final String HMAC_SHA512 = "HmacSHA512";

    private static final TypeReference<List<HmacKeyEntry>> hmacKeysTypeRef = new TypeReference<>() {
    };

    private static KeyEntry CURRENT_DB_HMAC_KEY;
    private static Map<String, SecretKey> HISTORY_DB_HMAC_KEY;

    //Avoid class instantiation
    private CryptoUtil() {
    }

    /**
     * Compute the HMAC of a given string.
     * The key is taken from the DB_HMAC_KEY environment variable
     *
     * @param input the string to HMAC
     * @return the HMAC value as a base64-encoded string
     */
    public static String computeHmacForDatabaseStorage(String input) {
        try {
            SecretKey dbHmacKey = getCtDatabaseHmacKey().key;
            Mac sha512Hmac = Mac.getInstance(HMAC_SHA512);
            sha512Hmac.init(dbHmacKey);
            byte[] macData = sha512Hmac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(macData);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new CloudtrustRuntimeException("Error while computing HMAC for database storage", e);
        }
    }

    private static KeyEntry getCtDatabaseHmacKey() {
        try {
            if (CURRENT_DB_HMAC_KEY == null) {
                List<HmacKeyEntry> keys = loadKeysFromEnvironment(DB_HMAC_KEY_ENV_VAR_NAME, hmacKeysTypeRef);
                if (keys.isEmpty()) {
                    throw new IllegalStateException("Cannot find an appropriate key from environment variable " +
                            DB_HMAC_KEY_ENV_VAR_NAME);
                }
                Collections.sort(keys);
                CURRENT_DB_HMAC_KEY = keys.get(0); // take the most recent key (top-most one)
                HISTORY_DB_HMAC_KEY = keys.stream().collect(Collectors.toMap(KeyEntry::getKid, KeyEntry::getKey));
            }
            return CURRENT_DB_HMAC_KEY;
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot load the " + DB_HMAC_KEY_ENV_VAR_NAME, ex);
        }
    }

    private static <T> T loadKeysFromEnvironment(String envVariableName, TypeReference<T> type) throws JsonProcessingException {
        String keys = System.getenv(envVariableName); // each key is encoded in Base64
        if (keys == null) {
            throw new IllegalStateException("Cannot load the environment variable" + envVariableName);
        }
        return new ObjectMapper().readValue(keys, type);
    }

    // package-protected method for clearing the keys, for test purpose
    static void clearKeys() {
        CURRENT_DB_HMAC_KEY = null;
        HISTORY_DB_HMAC_KEY = null;
    }
}
