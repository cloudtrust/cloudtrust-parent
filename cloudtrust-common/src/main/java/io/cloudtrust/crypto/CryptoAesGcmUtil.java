package io.cloudtrust.crypto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
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

public class CryptoAesGcmUtil {
    private static final String DB_ENCRYPTION_KEY_ENV_VAR_NAME = "DB_ENCRYPTION_KEY";
    private static final String AES_GCM_NOPADDING = "AES/GCM/NoPadding";
    private static final int IV_SIZE = 12; //size recommended by NIST

    private static final TypeReference<List<AesKeyEntry>> aesKeysTypeRef = new TypeReference<>() {};
    private static CryptoAesGcmUtil defaultInstance;

    private KeyEntry currentDbEncryptionKey;
    private Map<String, SecretKey> historyDbEncryptionKey;

    static {
        defaultInstance = fromEnvironmentNoException();
    }

    private CryptoAesGcmUtil(List<AesKeyEntry> keys) {
        if (keys.isEmpty()) {
            throw new IllegalStateException("Cannot find any appropriate key from configuration");
        }
        Collections.sort(keys);
        currentDbEncryptionKey = keys.get(0);
        historyDbEncryptionKey = keys.stream().collect(Collectors.toMap(KeyEntry::getKid, KeyEntry::getKey));
        if (defaultInstance == null) {
            defaultInstance = this;
        }
    }

    public static CryptoAesGcmUtil getDefault() {
        return defaultInstance;
    }

    public static CryptoAesGcmUtil fromEnvironmentNoException() {
        try {
            return fromEnvironment(DB_ENCRYPTION_KEY_ENV_VAR_NAME);
        } catch (JsonProcessingException|IllegalStateException e) {
            return null;
        }
    }

    public static CryptoAesGcmUtil fromEnvironment() throws JsonProcessingException {
        return fromEnvironment(DB_ENCRYPTION_KEY_ENV_VAR_NAME);
    }

    public static CryptoAesGcmUtil fromEnvironment(String envKeyName) throws JsonProcessingException {
        String keys = System.getenv(envKeyName); // each key is encoded in Base64
        if (keys == null || keys.isBlank()) {
            throw new IllegalStateException("Cannot find any appropriate key from environment variable " + envKeyName);
        }
        return fromJSON(keys);
    }

    public static CryptoAesGcmUtil fromJSON(String jsonContent) throws JsonProcessingException {
        if (jsonContent == null || jsonContent.isBlank()) {
            throw new IllegalStateException("Cannot find any appropriate key from json content");
        }
        return new CryptoAesGcmUtil(new ObjectMapper().readValue(jsonContent, aesKeysTypeRef));
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

    public String encryptForDatabaseStorage(String data) {
        return this.encryptForDatabaseStorage(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Encrypt data that are meant to be stored encrypted into the database
     *
     * @param data data to be encrypted
     * @return base64 representation of the encrypted data
     */
    public String encryptForDatabaseStorage(byte[] data) {
        try {
            SecretKey dbEncryptionKey = currentDbEncryptionKey.key;
            String encData = Base64.getEncoder().encodeToString(data);
            if (dbEncryptionKey != null && !dbEncryptionKey.getAlgorithm().equals("NONE")) {
                encData = gcmEncryptData(dbEncryptionKey, data);
            }
            EncryptedData encryptedData = new EncryptedData(currentDbEncryptionKey.kid, encData);
            return new ObjectMapper().writeValueAsString(encryptedData);
        } catch (BadPaddingException | IllegalBlockSizeException | JsonProcessingException ex) {
            throw new IllegalArgumentException("Unexpected error while encrypting data for database storage", ex);
        }
    }

    public String decryptFromDatabaseStorageToString(String data) {
        return new String(decryptFromDatabaseStorage(data), StandardCharsets.UTF_8);
    }

    /**
     * Decrypt data that are stored encrypted into the database
     *
     * @param data base64-encoded data to be decrypted
     * @return decrypted data as a UTF-8 encoded String
     */
    public byte[] decryptFromDatabaseStorage(String data) {
        try {
            // parse json structure
            EncryptedData encData = new ObjectMapper().readValue(data, EncryptedData.class);
            SecretKey dbEncryptionKey = historyDbEncryptionKey.get(encData.getKid());
            if (dbEncryptionKey == null) {
                // key cannot be found
                throw new IllegalStateException("Required key " + encData.getKid() + " cannot be found");
            }
            if (dbEncryptionKey.getEncoded().length == 1) {
                // empty key, no decryption necessary
                return Base64.getDecoder().decode(encData.getVal());
            }
            return gcmDecryptData(dbEncryptionKey, encData.getVal());
        } catch (BadPaddingException | IllegalBlockSizeException ex) {
            throw new IllegalArgumentException("Unexpected error while encrypting data for database storage", ex);
        } catch (JsonProcessingException ex) {
            // legacy: support for missing structure
            // try to decrypt with the current key
            try {
                SecretKey key = currentDbEncryptionKey.key;
                return gcmDecryptData(key, data);
            } catch (BadPaddingException | IllegalBlockSizeException | RuntimeException exc) {
                // if decryption fails, assumes that the data is in clear
                return data.getBytes(StandardCharsets.UTF_8);
            }
        }
    }
}
