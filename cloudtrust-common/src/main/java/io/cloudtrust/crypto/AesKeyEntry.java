package io.cloudtrust.crypto;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class AesKeyEntry extends KeyEntry {
    private static final String AES_KEY_TYPE = "AES";

    @Override
    SecretKey buildKeyFromBytesValue(byte[] value) {
        if (value == null) {
            return null;
        } else if (value.length == 0) {
            return new SecretKeySpec(new byte[]{0x01}, 0, 1, "NONE");
        } else {
            return new SecretKeySpec(value, 0, value.length, AES_KEY_TYPE);
        }
    }
}
