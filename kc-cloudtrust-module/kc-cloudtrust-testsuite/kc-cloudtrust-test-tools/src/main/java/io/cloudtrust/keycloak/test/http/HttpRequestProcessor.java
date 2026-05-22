package io.cloudtrust.keycloak.test.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudtrust.keycloak.test.util.JsonToolbox;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public interface HttpRequestProcessor {
    String method();

    String path();

    HeaderHandler headers();

    String param(String name);

    List<String> paramValues(String name);

    String body() throws IOException;

    default <T> T body(Class<T> classRef) throws IOException {
        String content = body();
        return StringUtils.isBlank(content) ? null : new ObjectMapper().readValue(content, classRef);
    }

    default <T> T body(TypeReference<T> typeRef) throws IOException {
        String content = body();
        return StringUtils.isBlank(content) ? null : new ObjectMapper().readValue(content, typeRef);
    }

    void statusCode(int status);

    void setHeader(String name, String value);

    void write(byte[] bytes);


    default void write(String data) {
        this.write(data.getBytes(StandardCharsets.UTF_8));
    }

    default void writeJson(Object obj) {
        this.setHeader("Content-Type", "application/json");
        this.write(JsonToolbox.toString(obj));
    }
}
