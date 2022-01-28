package io.cloudtrust.keycloak.models;

import com.fasterxml.jackson.core.JsonProcessingException;
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
    public void tryParseNullTest() {
        Assert.assertNull(AccreditationModel.tryParse(null));
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
        assertValid("{\"type\":\"XXX\", \"expiryDate\": \"31.12.2039\", \"creationMillis\": 1643379990000}");
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

    @Test
    public void toJSONTest() throws JsonProcessingException {
        // creationMillis should be ignored from JSON object and should not be transmitted by mappers
        Long timestamp = 1643379990000L;
        String date = "31.12.2039";
        String json = "{\"type\":\"XXX\", \"expiryDate\": \"DATE\", \"creationMillis\": VALUE}".replace("VALUE", Long.toString(timestamp)).replace("DATE", date);

        String updated = AccreditationModel.tryParse(json).toJSON();
        Assert.assertFalse(updated.contains(Long.toString(timestamp))); // timestamp (de/)serialized
        Assert.assertTrue(updated.contains("31.12.2039"));
    }
}
