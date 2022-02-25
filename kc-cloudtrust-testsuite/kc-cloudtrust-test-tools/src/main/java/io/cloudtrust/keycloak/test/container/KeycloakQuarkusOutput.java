package io.cloudtrust.keycloak.test.container;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.jboss.logging.Logger;

public class KeycloakQuarkusOutput {
    private static final Logger log = Logger.getLogger(KeycloakQuarkusOutput.class);

    private final InputStream stream;
	private StringBuilder console = new StringBuilder();

	public KeycloakQuarkusOutput(InputStream stream) {
		this.stream = stream;
	}

	public void clear() {
		console.setLength(0);
	}
	public String getConsoleOutput() throws IOException {
		sync();
		return console.toString();
	}

	private void sync() throws IOException {
		for (;;) {
			int len = stream.available();
			byte[] bytes = new byte[len];
			len = stream.read(bytes);
			if (len==0) {
				return;
			}
			String content = new String(bytes, 0, len, StandardCharsets.UTF_8);
			log.info("[KEYCLOAK] "+content);
			console.append(content);
		}
	}
}
