package io.cloudtrust.crypto;

import com.github.stefanbirkner.systemlambda.SystemLambda;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

class CryptoUtilHmacTest {
    private static final String DB_HMAC_KEY_ENV_VAR_NAME = "DB_HMAC_KEY";

    private String keyStructure;

    @BeforeEach
    public void init() {
        CryptoUtil.clearKeys();

        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[16];
        secureRandom.nextBytes(key);
        this.keyStructure = "[" +
                "{\"kid\": \"TEH_2\", \"value\": \"" + Base64.getEncoder().encodeToString(key) + "\"}," +
                "{\"kid\": \"TEH_1\", \"value\": \"T0xEX0tFWQ==\"}" +
                "]";
    }

    @Test
    void testSimpleHmac() throws Exception {
        String textToMac = "Test-String";
        SystemLambda.withEnvironmentVariable(DB_HMAC_KEY_ENV_VAR_NAME, this.keyStructure)
                .execute(() -> Assertions.assertDoesNotThrow(() -> CryptoUtil.computeHmacForDatabaseStorage(textToMac)));
    }


    @Test
    void testNoEnvVariableDefined() {
        // Assume that DB_HMAC_KEY_ENV_VAR_NAME env variable is not set by default
        String textToMac = "Test-String";
        Assertions.assertThrows(IllegalStateException.class, () -> CryptoUtil.computeHmacForDatabaseStorage(textToMac));
    }
}
