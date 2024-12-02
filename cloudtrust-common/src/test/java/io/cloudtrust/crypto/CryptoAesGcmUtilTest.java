package io.cloudtrust.crypto;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CryptoAesGcmUtilTest {
    @Test
    void testNullJsonContent() {
        assertThrows(IllegalStateException.class, () -> CryptoAesGcmUtil.fromJSON(null));
    }

    @Test
    void testBlankJsonContent() {
        assertThrows(IllegalStateException.class, () -> CryptoAesGcmUtil.fromJSON("    "));
    }

    @Test
    void testGsmDecryptNullData() throws BadPaddingException, IllegalBlockSizeException {
        assertThat(CryptoAesGcmUtil.gcmDecryptData(null, null), nullValue());
    }

    @Test
    void testSecretEncryptionDecryption() throws JsonProcessingException {
        byte[] plainText = new byte[32]; //256 bits
        new SecureRandom().nextBytes(plainText);
        CryptoAesGcmUtil codec = CryptoAesGcmUtil.fromEnvironment();
        assertThat(codec.decryptFromDatabaseStorage(codec.encryptForDatabaseStorage(plainText)), equalTo(plainText));
    }

    @Test
    void testSecretEncryptionDecryptionWithEmptyKey() throws Exception {
        byte[] plainText = "TEST".getBytes(StandardCharsets.UTF_8);
        CryptoAesGcmUtil codec = CryptoAesGcmUtil.fromJSON("[{\"kid\": \"TEE_3\", \"value\": \"\"}]");
        byte[] res = codec.decryptFromDatabaseStorage(codec.encryptForDatabaseStorage(plainText));
        assertThat(res, equalTo(plainText));
    }

    @Test
    void testDecryptionOfClearTextValue() throws JsonProcessingException {
        CryptoAesGcmUtil codec = CryptoAesGcmUtil.fromEnvironment();
        assertThat(codec.decryptFromDatabaseStorage("TEST_value"), equalTo("TEST_value".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void testDecryptionOfValidValueButMissingKid() throws IllegalBlockSizeException, BadPaddingException, JsonProcessingException {
        byte[] key = new byte[16];
        new SecureRandom().nextBytes(key);
        SecretKey secretKey = new SecretKeySpec(key, 0, key.length, "AES");
        String encryptedString = CryptoAesGcmUtil.gcmEncrypt(secretKey, "TEST_value");
        System.out.println("61> " + encryptedString);

        AesKeyEntry keyEntry = new AesKeyEntry();
        keyEntry.setKid("kc_dev_1");
        keyEntry.setValue(key);
        String jsonEntryKey = new ObjectMapper().writeValueAsString(List.of(keyEntry));
        System.out.println("67> " + jsonEntryKey);

        CryptoAesGcmUtil codec = CryptoAesGcmUtil.fromJSON(jsonEntryKey);
        assertThat(codec.decryptFromDatabaseStorage(encryptedString), equalTo("TEST_value".getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * test that the output fits by default in a VARCHAR(255) field in the DB
     *
     * @throws JsonProcessingException
     */
    @Test
    void testEncryptedSecretSize() throws JsonProcessingException {
        byte[] plainText = new byte[32]; //256 bits (key to be secured)
        new SecureRandom().nextBytes(plainText);
        CryptoAesGcmUtil codec = CryptoAesGcmUtil.fromEnvironment();
        String cipherStr = codec.encryptForDatabaseStorage(plainText);
        assertThat(cipherStr.length(), lessThan(255));
    }

    @Test
    void testGcmEncryptDecrypt() throws NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException {
        final String testString = "This is a test string to encrypt and decrypt!";
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey secretKey = keyGen.generateKey();
        String encryptedString = CryptoAesGcmUtil.gcmEncrypt(secretKey, testString);
        String decryptedString = CryptoAesGcmUtil.gcmDecrypt(secretKey, encryptedString);
        assertThat(testString, Matchers.is(decryptedString));
    }

    @Test
    void testMissingEnvVariableContent() throws JsonProcessingException {
        Assertions.assertThrows(IllegalStateException.class, () -> CryptoAesGcmUtil.fromEnvironment("ENV_VAR_DOES_NOT_EXIST"));
    }

    @Test
    void testInvalidEnvVariableContent() throws JsonProcessingException {
        Assertions.assertThrows(JsonParseException.class, () -> CryptoAesGcmUtil.fromEnvironment("PATH"));
    }

    @Test
    void testEmptyKeysContent() throws JsonProcessingException {
        Assertions.assertThrows(IllegalStateException.class, () -> CryptoAesGcmUtil.fromJSON("[]"));
    }

    @Test
    void testMissingKeyWhenDecrypting() throws Exception {
        String jsonKeys = "[" +
                "{\"kid\": \"TEE_3\", \"value\": \"T0xEX0tFWQ==\"}" +
                "]";
        String encValue = "{\"kid\": \"TEE_2\", \"val\": \"Test\"}";
        CryptoAesGcmUtil codec = CryptoAesGcmUtil.fromJSON(jsonKeys);
        Assertions.assertThrows(IllegalStateException.class, () -> codec.decryptFromDatabaseStorage(encValue));
    }

}
