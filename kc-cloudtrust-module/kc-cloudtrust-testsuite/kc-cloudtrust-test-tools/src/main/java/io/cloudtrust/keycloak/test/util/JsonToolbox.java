package io.cloudtrust.keycloak.test.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonToolbox {
    public static String toString(Object obj) {
        try {
            return obj == null ? "(null)" : new ObjectMapper().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }
}
