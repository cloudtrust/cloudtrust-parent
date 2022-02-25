package io.cloudtrust.keycloak.test.container;

import java.io.IOException;
import java.io.InputStream;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import io.cloudtrust.exception.CloudtrustRuntimeException;
import io.cloudtrust.keycloak.test.container.KeycloakQuarkusConfiguration.KeycloakQuarkusConfigurationBuilder;

public class KeycloakDeploy implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {
    private static final Logger LOG = Logger.getLogger(KeycloakDeploy.class);
    private static KeycloakDeploy singleton = null;
    private KeycloakQuarkusConfiguration kcConfig;
    private KeycloakQuarkusContainer keycloak;

    public KeycloakDeploy() throws IOException {
    	KeycloakDeploy.declareSingleton(this);
    	kcConfig = loadKeycloakConfiguration();
    }

    public static KeycloakDeploy get() {
		return KeycloakDeploy.singleton;
	}

    public static KeycloakQuarkusContainer getContainer() {
    	return KeycloakDeploy.singleton.keycloak;
    }

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
    	keycloak = KeycloakQuarkusContainer.start(kcConfig);
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopKeycloak));
        LOG.infof("Started Keycloak on %s", kcConfig.getBaseUrl());
	}

	private void stopKeycloak() {
    	if (this.keycloak!=null) {
    		synchronized (kcConfig) {
    			if (this.keycloak!=null) {
    	    		this.keycloak.stop();
    	    		this.keycloak = null;
    			}
    		}
    	}
	}

	@Override
	public void close() throws Throwable {
		stopKeycloak();
	}

    private static void declareSingleton(KeycloakDeploy value) {
    	KeycloakDeploy.singleton = value;
    }

    private KeycloakQuarkusConfiguration loadKeycloakConfiguration() throws IOException {
    	InputStream is = KeycloakDeploy.class.getClassLoader().getResourceAsStream("./keycloak.properties");
    	if (is==null) {
    		LOG.info("Did you forget to configure your Keycloak container using file src/test/resources/keycloak.properties ?");
    		throw new IOException("Missing file src/test/resources/keycloak.properties");
    	}
    	KeycloakQuarkusConfigurationBuilder cfg = KeycloakQuarkusConfiguration.createBuilder();
    	new ConfigurationReader().read(is, (s, k, v) -> {
			if ("build-arguments".equals(s)) {
				cfg.addBuildArgument("--"+k+"="+v);
			} else if ("properties".equals(s)) {
				cfg.addProperty(k, v);
			} else {
				throw new CloudtrustRuntimeException(s+" section does not support mapping");
			}
		}, (s, v) -> {
			if ("modules".equals(s)) {
				cfg.addModuleJar(v);
			} else if ("classes".equals(s)) {
				cfg.addClass(v);
			} else {
				throw new CloudtrustRuntimeException(s+" section does not support lists");
			}
		});
    	ConfigurationModel model = new ConfigurationModel();
    	cfg.addModulesJar(model.getModules());
    	cfg.addClasses(model.getClasses());
    	cfg.addProperties(model.getProperties());
		return cfg.build();
	}
}
