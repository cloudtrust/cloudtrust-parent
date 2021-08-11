package io.cloudtrust.crypto;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.lessThan;

public class CryptoUtilTest {

    private static final String DB_ENCRYPTION_KEY_ENV_VAR_NAME = "DB_ENCRYPTION_KEY";

    @Rule
    public final EnvironmentVariables envVariables = new EnvironmentVariables();

    private static final byte[] key = new byte[16];

    @Before
    public void init() {
        CryptoUtil.clearKeys();

        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(key);
        String keyStructure = "[" +
                "{\"kid\": \"TEE_1\", \"value\": \"T0xEX0tFWQ==\"}," +
                "{\"kid\": \"TEE_2\", \"value\": \"" + Base64.getEncoder().encodeToString(key) + "\"}" +
                "]";
        envVariables.set(DB_ENCRYPTION_KEY_ENV_VAR_NAME, keyStructure);
    }

    @Test
    public void testSecretEncryptionDecryption() {
        byte[] plainText = new byte[32]; //256 bits
        new SecureRandom().nextBytes(plainText);
        assertThat(CryptoUtil.decryptFromDatabaseStorage(CryptoUtil.encryptForDatabaseStorage(plainText)), equalTo(plainText));
    }

    @Test
    public void testSecretEncryptionDecryptionWithEmptyKey() {
        byte[] plainText = "TEST".getBytes(StandardCharsets.UTF_8);
        envVariables.set(DB_ENCRYPTION_KEY_ENV_VAR_NAME, "[{\"kid\": \"TEE_3\", \"value\": \"\"}]");
        assertThat(CryptoUtil.decryptFromDatabaseStorage(CryptoUtil.encryptForDatabaseStorage(plainText)), equalTo(plainText));
    }

    @Test
    public void testDecryptionOfClearTextValue() {
        assertThat(CryptoUtil.decryptFromDatabaseStorage("TEST_value"), equalTo("TEST_value".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testDecryptionOfValidValueButMissingKid() throws IllegalBlockSizeException, BadPaddingException {
        SecretKey secretKey = new SecretKeySpec(key, 0, key.length, "AES");
        String encryptedString = CryptoUtil.gcmEncrypt(secretKey, "TEST_value");
        assertThat(CryptoUtil.decryptFromDatabaseStorage(encryptedString), equalTo("TEST_value".getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * test that the output fits by default in a VARCHAR(255) field in the DB
     */
    @Test
    public void testEncryptedSecretSize() {
        byte[] plainText = new byte[32]; //256 bits (key to be secured)
        new SecureRandom().nextBytes(plainText);
        String cipherStr = CryptoUtil.encryptForDatabaseStorage(plainText);
        assertThat(cipherStr.length(), lessThan(255));
    }

    @Test
    public void testGcmEncryptDecrypt() throws NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException {
        final String testString = "This is a test string to encrypt and decrypt!";
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey secretKey = keyGen.generateKey();
        String encryptedString = CryptoUtil.gcmEncrypt(secretKey, testString);
        String decryptedString = CryptoUtil.gcmDecrypt(secretKey, encryptedString);
        assertThat(testString, Matchers.is(decryptedString));
    }

    @Test(expected = IllegalStateException.class)
    public void testNoEnvVariableDefined() {
        envVariables.clear(DB_ENCRYPTION_KEY_ENV_VAR_NAME);
        byte[] plainText = new byte[32];
        new SecureRandom().nextBytes(plainText);
        CryptoUtil.encryptForDatabaseStorage(plainText);
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingKeyWhenDecrypting() {
        envVariables.set(DB_ENCRYPTION_KEY_ENV_VAR_NAME, "[" +
                "{\"kid\": \"TEE_3\", \"value\": \"T0xEX0tFWQ==\"}" +
                "]");
        String encValue = "{\"kid\": \"TEE_2\", \"val\": \"Test\"}";
        CryptoUtil.decryptFromDatabaseStorage(encValue);
    }

}
