package io.cloudtrust.keycloak.test.container;

import io.cloudtrust.exception.CloudtrustRuntimeException;
import io.cloudtrust.keycloak.test.container.ConfigurationReader.TriConsumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

class ConfigurationReaderTest {
    private void readKeyValueFails(String section, String key, String value) {
        Assertions.fail("should not execute this");
    }

    private void readSingleValueFails(String section, String value) {
        Assertions.fail("should not execute this");
    }

    private void readConfig(String input, TriConsumer<String, String, String> whenPairRead, BiConsumer<String, String> whenValueRead) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(input.getBytes());
        ConfigurationReader cr = new ConfigurationReader();
        cr.read(bais, whenPairRead, whenValueRead);
    }

    @Test
    void simpleReadTest() throws IOException {
        String input = "# comment\n"
                + "[section1]\n"
                + "plain-row-value\n"
                + "\n"
                + "[section2]\n"
                + "key1=value1\n"
                + "key2=value2";
        AtomicInteger callbackCount = new AtomicInteger();
        readConfig(input, (s, k, v) -> {
            callbackCount.incrementAndGet();
            Assertions.assertEquals("section2", s);
            Assertions.assertTrue(k.startsWith("key"));
            Assertions.assertEquals(k.replace("key", "value"), v);
        }, (s, v) -> {
            callbackCount.incrementAndGet();
            Assertions.assertEquals("section1", s);
            Assertions.assertEquals("plain-row-value", v);
        });
        Assertions.assertEquals(3, callbackCount.get());
    }

    @Test
    void valueWithoutSectionTest() {
        Assertions.assertThrows(IOException.class, () -> readConfig("key=value-without-section", this::readKeyValueFails, this::readSingleValueFails));
    }

    @Test
    void unknownFunctionTest() {
        Assertions.assertThrows(CloudtrustRuntimeException.class, () ->
                readConfig("[section]\nkey=func:unknown()", this::readKeyValueFails, this::readSingleValueFails)
        );
    }

    @Test
    void functionBase64Test() throws IOException {
        readConfig("[section]\nkey=func:base64(value)", (s, k, v) -> {
            Assertions.assertEquals("section", s);
            Assertions.assertEquals("key", k);
            Assertions.assertEquals("dmFsdWU=", v);
        }, this::readSingleValueFails);
    }

    @Test
    void functionRandomBytesTest() throws IOException {
        Map<String, String> values = new HashMap<>();
        readConfig("[section]\nkey1=func:randombytes(16)\nkey2=func:randombytes(16)", (s, k, v) -> {
            Assertions.assertEquals("section", s);
            Assertions.assertTrue("key1".equals(k) || "key2".equals(k));
            Assertions.assertEquals(16, v.length());
            values.put(k, v);
        }, this::readSingleValueFails);
        Assertions.assertEquals(2, values.size());
        Assertions.assertNotEquals(values.get("key1"), values.get("key2"));
    }

    @Test
    void functionInvalidNumberParameterTest() {
        Assertions.assertThrows(CloudtrustRuntimeException.class, () ->
                readConfig("[section]\nkey=func:randombytes(abc)", this::readKeyValueFails, this::readSingleValueFails)
        );
    }

    @Test
    void composeFunctionsTest() throws IOException {
        readConfig("[section]\ncompose=func:base64(func:randombytes(16))", (s, k, v) -> {
            Assertions.assertEquals("section", s);
            Assertions.assertEquals("compose", k);
            String decoded = new String(Base64.getDecoder().decode(v));
            Assertions.assertEquals(16, decoded.length());
        }, this::readSingleValueFails);
    }

    @Test
    void fileFunctionTest() throws IOException {
        readConfig("[section]\nfileContent=func:file(dummy.txt)", (s, k, v) -> {
            Assertions.assertEquals("section", s);
            Assertions.assertEquals("fileContent", k);
            Assertions.assertEquals("ConfigurationReader rocks!!!", v);
        }, this::readSingleValueFails);
    }

    @Test
    void unknownFileFunctionTest() {
        Assertions.assertThrows(CloudtrustRuntimeException.class, () ->
                readConfig("[section]\nfileContent=func:file(where-is-my-file.txt)", this::readKeyValueFails, this::readSingleValueFails)
        );
    }
}
