package io.cloudtrust.keycloak.test.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudtrust.keycloak.test.container.KeycloakQuarkusConfiguration;
import org.apache.commons.lang3.math.NumberUtils;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class TestSuiteParameters {
    private static final Logger log = Logger.getLogger(TestSuiteParameters.class);
    private static final TypeReference<Map<String, Object>> mapTypeRef = new TypeReference<>() {
    };

    private static final TestSuiteParameters singleton = new TestSuiteParameters();

    public static TestSuiteParameters get() {
        return TestSuiteParameters.singleton;
    }

    private final Map<String, Object> environment = new HashMap<>();

    private TestSuiteParameters() {
        this.loadDefaultConfiguration();
    }

    private void loadDefaultConfiguration() {
        InputStream res = KeycloakQuarkusConfiguration.class.getClassLoader().getResourceAsStream("keycloak-env.json");
        if (res == null) {
            log.info("No configuration file found");
            return;
        }
        try {
            this.environment.putAll(new ObjectMapper().readValue(res, mapTypeRef));
            this.environment.forEach((k, v) -> log.info("Loaded env: " + k + "=" + v));
        } catch (IOException e) {
            log.error("Could not read configuration", e);
        }
    }

    public String getEnv(String paramName, String defaultValue) {
        Object res = System.getenv(paramName);
        if (res == null) {
            res = this.environment.get(paramName);
        }
        if (res == null) {
            return defaultValue;
        }
        return res.toString();
    }

    public Integer getEnvAsInt(String paramName, Integer defaultValue) {
        String value = this.getEnv(paramName, null);
        Integer res = NumberUtils.toInt(value, -999);
        return res != -999 ? res : defaultValue;
    }
}
