package io.cloudtrust.crypto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EncryptedData {
    @JsonProperty("kid")
    private String kid;

    @JsonProperty("val")
    private String val;

    @JsonCreator
    EncryptedData(@JsonProperty("kid") String kid, @JsonProperty("val") String val) {
        this.kid = kid;
        this.val = val;
    }

    public String getKid() {
        return kid;
    }

    public void setKid(String kid) {
        this.kid = kid;
    }

    public String getVal() {
        return val;
    }

    public void setVal(String val) {
        this.val = val;
    }
}
