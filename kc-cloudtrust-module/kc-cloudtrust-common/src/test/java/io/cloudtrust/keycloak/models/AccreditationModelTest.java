package io.cloudtrust.keycloak.models;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

class AccreditationModelTest {
    @Test
    void tryParseTest() {
        AccreditationModel accred = AccreditationModel.tryParse("{\"type\": \"value type\"}");
        Assertions.assertNotNull(accred);
        Assertions.assertEquals("value type", accred.getType());
        Assertions.assertNull(accred.getExpiryDate());
        Assertions.assertNull(accred.isRevoked());
    }

    @Test
    void tryParseNullTest() {
        Assertions.assertNull(AccreditationModel.tryParse(null));
    }

    @Test
    void tryParseFailsTest() {
        Assertions.assertNull(AccreditationModel.tryParse("{"));
    }

    @Test
    void isValidTest() {
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
        Assertions.assertNotNull(accred);
        Assertions.assertFalse(accred.isValid());
    }

    private void assertValid(String json) {
        AccreditationModel accred = AccreditationModel.tryParse(json);
        Assertions.assertNotNull(accred);
        Assertions.assertTrue(accred.isValid());
    }

    @Test
    void toJSONTest() throws JsonProcessingException {
        // creationMillis should be ignored from JSON object and should not be transmitted by mappers
        Long timestamp = 1643379990000L;
        String date = "31.12.2039";
        String json = "{\"type\":\"XXX\", \"expiryDate\": \"DATE\", \"creationMillis\": VALUE}".replace("VALUE", Long.toString(timestamp)).replace("DATE", date);

        String updated = AccreditationModel.tryParse(json).toJSON();
        Assertions.assertFalse(updated.contains(Long.toString(timestamp))); // timestamp (de/)serialized
        Assertions.assertTrue(updated.contains("31.12.2039"));
    }
}
