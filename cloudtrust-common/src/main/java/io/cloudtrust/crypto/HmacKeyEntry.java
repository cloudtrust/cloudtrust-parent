package io.cloudtrust.crypto;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class HmacKeyEntry extends KeyEntry {
    private static final String HMAC_SHA512_KEY_TYPE = "HmacSHA512";

    @Override
    SecretKey buildKeyFromBytesValue(byte[] value) {
        if (value == null) {
            return null;
        } else {
            return new SecretKeySpec(value, HMAC_SHA512_KEY_TYPE);
        }
    }
}
