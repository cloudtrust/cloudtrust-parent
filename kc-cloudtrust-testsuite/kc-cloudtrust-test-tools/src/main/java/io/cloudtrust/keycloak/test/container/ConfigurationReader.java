package io.cloudtrust.keycloak.test.container;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigurationReader {
	private Pattern patternSection = Pattern.compile("^\\[([\\w-]+)\\]$");
	private Pattern patternProperty = Pattern.compile("^([^=]+)=(.*)$");

	public interface TriConsumer<T, U, V> {
		void accept(T t, U u, V v);
	}

	public void read(InputStream is, TriConsumer<String, String, String> mapValue, BiConsumer<String, String> arrayValue) throws IOException {
    	try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
    		String line;
    		String section = null;
    		while ((line = br.readLine())!=null) {
    			line = line.trim();
    			if (line.startsWith("#") || line.isEmpty()) {
    				continue;
    			}
    			Matcher m = patternSection.matcher(line);
    			if (m.find()) {
    				section = m.group(1);
    			} else if (section==null) {
    				throw new IOException("Missing section");
    			} else {
    				m = patternProperty.matcher(line);
    				if (m.find()) {
    					mapValue.accept(section, m.group(1).trim(), m.group(2).trim());
    				} else {
    					arrayValue.accept(section, line.trim());
    				}
    			}
    		}
    	}
	}
}
