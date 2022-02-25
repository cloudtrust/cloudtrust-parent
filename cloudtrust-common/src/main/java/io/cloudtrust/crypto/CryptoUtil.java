package io.cloudtrust.crypto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudtrust.exception.CloudtrustRuntimeException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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
    private static final String DB_ENCRYPTION_KEY_ENV_VAR_NAME = "DB_ENCRYPTION_KEY";

    private static final String AES_GCM_NOPADDING = "AES/GCM/NoPadding";
    private static final String HMAC_SHA512 = "HmacSHA512";
    private static final int IV_SIZE = 12; //size recommended by NIST

    private static final TypeReference<List<AesKeyEntry>> aesKeysTypeRef = new TypeReference<List<AesKeyEntry>>() {
    };
    private static final TypeReference<List<HmacKeyEntry>> hmacKeysTypeRef = new TypeReference<List<HmacKeyEntry>>() {
    };

    private static KeyEntry CURRENT_DB_HMAC_KEY;
    private static Map<String, SecretKey> HISTORY_DB_HMAC_KEY;
    private static KeyEntry CURRENT_DB_ENCRYPTION_KEY;
    private static Map<String, SecretKey> HISTORY_DB_ENCRYPTION_KEY;

    //Avoid class instantiation
    private CryptoUtil() {
    }

    /**
     * Encrypts the given String with the AES/GCM/NoPadding algorithm.
     *
     * @param aesKey        The AES secret key to encrypt the data
     * @param textToEncrypt The text to encrypt
     * @return The encrypted output encoded to a base64 String.
     * @throws BadPaddingException       thrown if there's a problem with the submitted data
     * @throws IllegalBlockSizeException thrown if there's a problem with the submitted data
     */
    public static String gcmEncrypt(SecretKey aesKey, String textToEncrypt) throws BadPaddingException, IllegalBlockSizeException {
        return gcmEncryptData(aesKey, textToEncrypt.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Encrypts the given byte array with the AES/GCM/NoPadding algorithm.
     *
     * @param aesKey The AES secret key to encrypt the data
     * @param data   The bytes to encrypt
     * @return The encrypted output encoded to a base64 String.
     * @throws BadPaddingException       thrown if there's a problem with the submitted data
     * @throws IllegalBlockSizeException thrown if there's a problem with the submitted data
     */
    public static String gcmEncryptData(SecretKey aesKey, byte[] data) throws BadPaddingException, IllegalBlockSizeException {
        SecureRandom sr = new SecureRandom();
        byte[] iv = new byte[IV_SIZE];
        sr.nextBytes(iv);
        try {
            final Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, parameterSpec);
            byte[] cipheredText = cipher.doFinal(data);
            ByteBuffer gcmData = ByteBuffer.allocate(iv.length + cipheredText.length);
            gcmData.put(iv);
            gcmData.put(cipheredText);
            return Base64.getEncoder().encodeToString(gcmData.array());
        } catch (InvalidKeyException | InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new IllegalStateException("Error in the gcm encryption algorithm parameters", e);
        }
    }

    /**
     * Decrypts the given base64-encoded string with the AES/GCM/NoPadding algorithm.
     *
     * @param aesKey        The AES secret key used to encrypt the data
     * @param encryptedData The data to decrypt, encoded in a base64 String
     * @return The decrypted data, or null if the provided data is null
     * @throws BadPaddingException       thrown if there's a problem with the submitted data
     * @throws IllegalBlockSizeException thrown if there's a problem with the submitted data
     */
    public static byte[] gcmDecryptData(SecretKey aesKey, String encryptedData) throws BadPaddingException, IllegalBlockSizeException {
        if (encryptedData == null) {
        	return null;
        }
        ByteBuffer gcmData = ByteBuffer.wrap(Base64.getDecoder().decode(encryptedData));
        byte[] iv = new byte[IV_SIZE];
        gcmData.get(iv);
        byte[] cipheredText = new byte[gcmData.remaining()];
        gcmData.get(cipheredText);
        try {
            Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, parameterSpec);
            return cipher.doFinal(cipheredText);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new IllegalStateException("Error in the gcm decryption algorithm parameters", e);
        }
    }

    /**
     * Decrypts the given String with the AES/GCM/NoPadding algorithm.
     *
     * @param aesKey        The AES secret key used to encrypt the data
     * @param encryptedText The text to encrypt, encoded in a base64 String
     * @return The decrypted plain text
     * @throws BadPaddingException       thrown if there's a problem with the submitted data
     * @throws IllegalBlockSizeException thrown if there's a problem with the submitted data
     */
    public static String gcmDecrypt(SecretKey aesKey, String encryptedText) throws BadPaddingException, IllegalBlockSizeException {
        return new String(gcmDecryptData(aesKey, encryptedText), StandardCharsets.UTF_8);
    }

    /**
     * Encrypt data that are meant to be stored encrypted into the database
     *
     * @param data data to be encrypted
     * @return base64 representation of the encrypted data
     */
    public static String encryptForDatabaseStorage(byte[] data) {
        try {
            SecretKey dbEncryptionKey = getCtDatabaseEncryptionKey().key;
            String encData = Base64.getEncoder().encodeToString(data);
            if (dbEncryptionKey != null && !dbEncryptionKey.getAlgorithm().equals("NONE")) {
                encData = gcmEncryptData(dbEncryptionKey, data);
            }
            EncryptedData encryptedData = new EncryptedData(getCtDatabaseEncryptionKey().kid, encData);
            return new ObjectMapper().writeValueAsString(encryptedData);
        } catch (BadPaddingException | IllegalBlockSizeException | JsonProcessingException ex) {
            throw new IllegalArgumentException("Unexpected error while encrypting data for database storage", ex);
        }
    }

    /**
     * Decrypt data that are stored encrypted into the database
     *
     * @param data base64-encoded data to be decrypted
     * @return decrypted data as a UTF-8 encoded String
     */
    public static byte[] decryptFromDatabaseStorage(String data) {
        try {
            // ensure keys are loaded
            getCtDatabaseEncryptionKey();
            // parse json structure
            EncryptedData encData = new ObjectMapper().readValue(data, EncryptedData.class);
            SecretKey dbEncryptionKey = HISTORY_DB_ENCRYPTION_KEY.get(encData.kid);
            if (dbEncryptionKey == null) {
                // key cannot be found
                throw new IllegalStateException("Required key " + encData.kid + " cannot be found");
            }
            if (dbEncryptionKey.getEncoded().length == 1) {
                // empty key, no decryption necessary
                return Base64.getDecoder().decode(encData.val);
            }
            return gcmDecryptData(dbEncryptionKey, encData.val);
        } catch (BadPaddingException | IllegalBlockSizeException ex) {
            throw new IllegalArgumentException("Unexpected error while encrypting data for database storage", ex);
        } catch (JsonProcessingException ex) {
            // legacy: support for missing structure
            // try to decrypt with the current key
            try {
                SecretKey key = getCtDatabaseEncryptionKey().key;
                return gcmDecryptData(key, data);
            } catch (BadPaddingException | IllegalBlockSizeException | RuntimeException exc) {
                // if decryption fails, assumes that the data is in clear
                return data.getBytes(StandardCharsets.UTF_8);
            }
        }
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

    private static KeyEntry getCtDatabaseEncryptionKey() {
        try {
            if (CURRENT_DB_ENCRYPTION_KEY == null) {
                List<AesKeyEntry> keys = loadKeysFromEnvironment(DB_ENCRYPTION_KEY_ENV_VAR_NAME, aesKeysTypeRef);
                if (keys.isEmpty()) {
                    throw new IllegalStateException("Cannot find any appropriate key from environment variable " +
                            DB_ENCRYPTION_KEY_ENV_VAR_NAME);
                }
                // sort list of keys according to indices (bigger to smaller, to have the latest first)
                Collections.sort(keys);
                CURRENT_DB_ENCRYPTION_KEY = keys.get(0);
                HISTORY_DB_ENCRYPTION_KEY = keys.stream().collect(Collectors.toMap(KeyEntry::getKid, KeyEntry::getKey));
            }
            return CURRENT_DB_ENCRYPTION_KEY;
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot load the " + DB_ENCRYPTION_KEY_ENV_VAR_NAME, ex);
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

    static class EncryptedData {
        @JsonProperty("kid")
        String kid;
        @JsonProperty("val")
        String val;

        @JsonCreator
        EncryptedData(@JsonProperty("kid") String kid, @JsonProperty("val") String val) {
            this.kid = kid;
            this.val = val;
        }
    }

    // package-protected method for clearing the keys, for test purpose
    static void clearKeys() {
        CURRENT_DB_HMAC_KEY = null;
        HISTORY_DB_HMAC_KEY = null;
        CURRENT_DB_ENCRYPTION_KEY = null;
        HISTORY_DB_ENCRYPTION_KEY = null;
    }
}
