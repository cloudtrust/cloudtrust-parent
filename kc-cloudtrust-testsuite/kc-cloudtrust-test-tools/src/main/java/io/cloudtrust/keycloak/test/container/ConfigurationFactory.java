package io.cloudtrust.keycloak.test.container;

import io.cloudtrust.exception.CloudtrustRuntimeException;
import io.cloudtrust.keycloak.test.container.KeycloakQuarkusConfiguration.KeycloakQuarkusConfigurationBuilder;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationFactory {
    private static final Logger LOG = Logger.getLogger(ConfigurationFactory.class);
    private static final ConfigurationFactory singleton = new ConfigurationFactory();

    private final Map<String, KeycloakQuarkusConfiguration> loadedConfs = new HashMap<>();
    private KeycloakQuarkusConfiguration defaultConf = null;

    public static ConfigurationFactory get() {
        return singleton;
    }

    public KeycloakQuarkusConfiguration getDefault() {
        return this.defaultConf;
    }

    public KeycloakQuarkusConfiguration getConfiguration(String filename) {
        KeycloakQuarkusConfiguration res = this.loadedConfs.get(filename);
        if (res == null) {
            res = this.load(filename);
            if (this.defaultConf == null && this.loadedConfs.isEmpty()) {
                this.defaultConf = res;
            }
            this.loadedConfs.put(filename, res);
        }
        return res;
    }

    private KeycloakQuarkusConfiguration load(String filename) {
        InputStream is = KeycloakDeploy.class.getClassLoader().getResourceAsStream(filename);
        if (is == null) {
            LOG.info("Did you forget to configure your Keycloak container using file src/test/resources/keycloak.properties ?");
            throw new CloudtrustRuntimeException("Missing file src/test/resources/keycloak.properties");
        }
        KeycloakQuarkusConfigurationBuilder cfg = KeycloakQuarkusConfiguration.createBuilder();
        try {
            new ConfigurationReader().read(is, (s, k, v) -> {
                if ("build-arguments".equals(s)) {
                    cfg.addBuildArgument("--" + k + "=" + v);
                } else if ("properties".equals(s)) {
                    cfg.addProperty(k, v);
                } else if ("environment".equals(s)) {
                    cfg.addEnvironment(k, v);
                } else {
                    throw new CloudtrustRuntimeException(s + " section does not support mapping");
                }
            }, (s, v) -> {
                if ("modules".equals(s)) {
                    cfg.addModuleJar(v);
                } else if ("classpath".equals(s)) {
                    cfg.addClasspath(v);
                } else {
                    throw new CloudtrustRuntimeException(s + " section does not support lists");
                }
            });
        } catch (IOException e) {
            throw new CloudtrustRuntimeException("Can't read configuration file " + filename, e);
        }
        return cfg.build();
    }
}
