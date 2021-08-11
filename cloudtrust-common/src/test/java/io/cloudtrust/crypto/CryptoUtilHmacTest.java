package io.cloudtrust.crypto;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.security.SecureRandom;
import java.util.Base64;

public class CryptoUtilHmacTest {

    private static final String DB_HMAC_KEY_ENV_VAR_NAME = "DB_HMAC_KEY";

    @Rule
    public final EnvironmentVariables envVariables = new EnvironmentVariables();

    @Before
    public void init() {
        CryptoUtil.clearKeys();

        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[16];
        secureRandom.nextBytes(key);
        String keyStructure = "[" +
                "{\"kid\": \"TEH_2\", \"value\": \"" + Base64.getEncoder().encodeToString(key) + "\"}," +
                "{\"kid\": \"TEH_1\", \"value\": \"T0xEX0tFWQ==\"}" +
                "]";
        envVariables.set(DB_HMAC_KEY_ENV_VAR_NAME, keyStructure);
    }

    @Test
    public void testSimpleHmac() {
        String textToMac = "Test-String";
        CryptoUtil.computeHmacForDatabaseStorage(textToMac);
    }


    @Test(expected = IllegalStateException.class)
    public void testNoEnvVariableDefined() {
        envVariables.clear(DB_HMAC_KEY_ENV_VAR_NAME);
        String textToMac = "Test-String";
        CryptoUtil.computeHmacForDatabaseStorage(textToMac);
    }

}
