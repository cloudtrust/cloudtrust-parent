package io.cloudtrust.keycloak.test.http;

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public interface HttpRequestProcessor {
    String method();
    String path();
    HeaderHandler headers();
    String param(String name);
    List<String> paramValues(String name);
    String body() throws IOException;
    <T> T body(Class<T> classRef) throws IOException;
    <T> T body(TypeReference<T> typeRef) throws IOException;

    void statusCode(int status);
    void setHeader(String name, String value);
    void write(byte[] bytes) throws IOException;
    void write(String data) throws IOException;
    void writeJson(Object obj) throws IOException;
    OutputStream output();
}
