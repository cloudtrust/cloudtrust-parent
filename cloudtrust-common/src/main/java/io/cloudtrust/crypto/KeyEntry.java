package io.cloudtrust.crypto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.crypto.SecretKey;

abstract class KeyEntry implements Comparable<KeyEntry> {
    @JsonProperty("kid")
    String kid;
    @JsonProperty("value")
    byte[] value;
    @JsonIgnore
    int priority;
    @JsonIgnore
    SecretKey key;

    public String getKid() {
        return kid;
    }

    public void setKid(String kid) {
        this.kid = kid;
        priority = Integer.parseInt(kid.substring(kid.lastIndexOf("_") + 1));
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
        key = buildKeyFromBytesValue(value);
    }

    public SecretKey getKey() {
        return key;
    }

    abstract SecretKey buildKeyFromBytesValue(byte[] value);

    @Override
    public int compareTo(KeyEntry ke) {
        return Integer.compare(ke.priority, this.priority);
    }
}
