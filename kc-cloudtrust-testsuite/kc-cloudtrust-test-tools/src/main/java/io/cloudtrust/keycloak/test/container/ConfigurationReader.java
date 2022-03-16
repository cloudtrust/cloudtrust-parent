package io.cloudtrust.keycloak.test.container;

import io.cloudtrust.exception.CloudtrustRuntimeException;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigurationReader {
    private final Pattern patternSection = Pattern.compile("^\\[([\\w-]+)\\]$");
    private final Pattern patternProperty = Pattern.compile("^([^=]+)=(.*)$");
    private final Pattern patternFunction = Pattern.compile("func:([a-z0-9]+)\\(([^\\)]*)\\)");

    public interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }

    public void read(InputStream is, TriConsumer<String, String, String> mapValue, BiConsumer<String, String> arrayValue) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            String section = null;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }
                Matcher m = patternSection.matcher(line);
                if (m.find()) {
                    section = m.group(1);
                } else if (section == null) {
                    throw new IOException("Missing section");
                } else {
                    m = patternProperty.matcher(line);
                    if (m.find()) {
                        mapValue.accept(section, m.group(1).trim(), eval(m.group(2).trim()));
                    } else {
                        arrayValue.accept(section, line.trim());
                    }
                }
            }
        }
    }

    private String eval(String value) {
        for (; ; ) {
            Matcher m = patternFunction.matcher(value);
            int pos = value.lastIndexOf("func:");
            if (pos < 0 || !m.find(pos)) {
                return value;
            }
            String capture = m.group();
            String newValue = resolve(m.group(1), m.group(2));
            value = value.replace(capture, newValue);
        }
    }

    private String resolve(String funcName, String params) {
        if ("base64".equals(funcName)) {
            return org.keycloak.common.util.Base64.encodeBytes(params.getBytes());
        } else if ("randombytes".equals(funcName)) {
            return createRandomBytes(toNumber(params));
        } else if ("file".equals(funcName)) {
            return readFile(params);
        }
        throw new CloudtrustRuntimeException("Unknown function " + funcName + " (parameters: " + params + ")");
    }

    private String createRandomBytes(int number) {
        // Creates a random value but ensure it is possible to store it as a String
        String available = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < number; i++) {
            char c = available.charAt(secureRandom.nextInt(available.length()));
            builder.append(c);
        }
        return builder.toString();
    }

    private int toNumber(String value) {
        if (!NumberUtils.isCreatable(value)) {
            throw new CloudtrustRuntimeException("Can't parse number '" + value + "'");
        }
        return NumberUtils.createInteger(value);
    }

    private String readFile(String filename) {
        InputStream is = ConfigurationReader.class.getClassLoader().getResourceAsStream(filename);
        if (is == null) {
            throw new CloudtrustRuntimeException("Can't find file " + filename);
        }
        try {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new CloudtrustRuntimeException("Can't read file " + filename, e);
        }
    }
}
