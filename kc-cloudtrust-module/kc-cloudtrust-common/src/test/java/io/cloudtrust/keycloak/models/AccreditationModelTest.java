package io.cloudtrust.keycloak.models;

import org.junit.Assert;
import org.junit.Test;

public class AccreditationModelTest {
    @Test
    public void tryParseTest() {
        AccreditationModel accred = AccreditationModel.tryParse("{\"type\": \"value type\"}");
        Assert.assertNotNull(accred);
        Assert.assertEquals("value type", accred.getType());
        Assert.assertNull(accred.getExpiryDate());
        Assert.assertNull(accred.isRevoked());
    }

    @Test
    public void tryParseFailsTest() {
        Assert.assertNull(AccreditationModel.tryParse("{"));
    }

    @Test
    public void isValidTest() {
        assertInvalid("{}");
        assertInvalid("{\"type\":\"XXX\"}");
        assertInvalid("{\"type\":\"XXX\", \"revoked\": true}");
        assertInvalid("{\"type\":\"XXX\", \"revoked\": false}");
        assertInvalid("{\"type\":\"XXX\", \"revoked\": false, \"expiryDate\": \"not-a-date\"}");
        assertInvalid("{\"type\":\"XXX\", \"revoked\": false, \"expiryDate\": \"31.12.2019\"}");
        assertValid("{\"type\":\"XXX\", \"expiryDate\": \"31.12.2039\"}");
    }

    private void assertInvalid(String json) {
        AccreditationModel accred = AccreditationModel.tryParse(json);
        Assert.assertNotNull(accred);
        Assert.assertFalse(accred.isValid());
    }

    private void assertValid(String json) {
        AccreditationModel accred = AccreditationModel.tryParse(json);
        Assert.assertNotNull(accred);
        Assert.assertTrue(accred.isValid());
    }
}
