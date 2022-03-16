package io.cloudtrust.crypto;

import com.github.stefanbirkner.systemlambda.SystemLambda;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.lessThan;

class CryptoUtilTest {
    private static final String DB_ENCRYPTION_KEY_ENV_VAR_NAME = "DB_ENCRYPTION_KEY";

    private static final byte[] key = new byte[16];

    @BeforeEach
    public void init() {
        CryptoUtil.clearKeys();
    }

    @Test
    void testSecretEncryptionDecryption() {
        byte[] plainText = new byte[32]; //256 bits
        new SecureRandom().nextBytes(plainText);
        assertThat(CryptoUtil.decryptFromDatabaseStorage(CryptoUtil.encryptForDatabaseStorage(plainText)), equalTo(plainText));
    }

    @Test
    void testSecretEncryptionDecryptionWithEmptyKey() throws Exception {
        byte[] plainText = "TEST".getBytes(StandardCharsets.UTF_8);
        byte[] res = SystemLambda
                .withEnvironmentVariable(DB_ENCRYPTION_KEY_ENV_VAR_NAME, "[{\"kid\": \"TEE_3\", \"value\": \"\"}]")
                .execute(() -> CryptoUtil.decryptFromDatabaseStorage(CryptoUtil.encryptForDatabaseStorage(plainText)));
        assertThat(res, equalTo(plainText));
    }

    @Test
    void testDecryptionOfClearTextValue() {
        assertThat(CryptoUtil.decryptFromDatabaseStorage("TEST_value"), equalTo("TEST_value".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void testDecryptionOfValidValueButMissingKid() throws IllegalBlockSizeException, BadPaddingException {
        SecretKey secretKey = new SecretKeySpec(key, 0, key.length, "AES");
        String encryptedString = CryptoUtil.gcmEncrypt(secretKey, "TEST_value");
        assertThat(CryptoUtil.decryptFromDatabaseStorage(encryptedString), equalTo("TEST_value".getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * test that the output fits by default in a VARCHAR(255) field in the DB
     */
    @Test
    void testEncryptedSecretSize() {
        byte[] plainText = new byte[32]; //256 bits (key to be secured)
        new SecureRandom().nextBytes(plainText);
        String cipherStr = CryptoUtil.encryptForDatabaseStorage(plainText);
        assertThat(cipherStr.length(), lessThan(255));
    }

    @Test
    void testGcmEncryptDecrypt() throws NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException {
        final String testString = "This is a test string to encrypt and decrypt!";
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey secretKey = keyGen.generateKey();
        String encryptedString = CryptoUtil.gcmEncrypt(secretKey, testString);
        String decryptedString = CryptoUtil.gcmDecrypt(secretKey, encryptedString);
        assertThat(testString, Matchers.is(decryptedString));
    }

    @Test
    void testNoEnvVariableDefined() {
        // By default, DB_ENCRYPTION_KEY_ENV_VAR_NAME env variable is not set
        byte[] plainText = new byte[32];
        new SecureRandom().nextBytes(plainText);
        Assertions.assertThrows(IllegalStateException.class, () -> CryptoUtil.encryptForDatabaseStorage(plainText));
    }

    @Test
    void testMissingKeyWhenDecrypting() throws Exception {
        String envValue = "[" +
                "{\"kid\": \"TEE_3\", \"value\": \"T0xEX0tFWQ==\"}" +
                "]";
        String encValue = "{\"kid\": \"TEE_2\", \"val\": \"Test\"}";
        SystemLambda.withEnvironmentVariable(DB_ENCRYPTION_KEY_ENV_VAR_NAME, envValue)
                .execute(() -> Assertions.assertThrows(IllegalStateException.class, () -> CryptoUtil.decryptFromDatabaseStorage(encValue)));
    }

}
