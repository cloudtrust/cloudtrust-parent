package io.cloudtrust.keycloak.test.http;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.List;

public class HttpRequestProcessorImpl implements HttpRequestProcessor {
    private final HttpServerExchange exchange;
    private boolean blockingStarted = false;
    private String body = null;

    public HttpRequestProcessorImpl(HttpServerExchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public String method() {
        return this.exchange.getRequestMethod().toString();
    }

    @Override
    public String path() {
        return this.exchange.getRequestPath();
    }

    @Override
    public HeaderHandler headers() {
        return new HeaderHandlerImpl(exchange);
    }

    @Override
    public String param(String name) {
        Deque<String> params = exchange.getQueryParameters().get(name);
        return params != null ? params.getFirst() : null;
    }

    @Override
    public List<String> paramValues(String name) {
        Deque<String> params = exchange.getQueryParameters().get(name);
        return params != null ? List.copyOf(params) : List.of();
    }

    @Override
    public String body() throws IOException {
        if (this.body == null) {
            this.startBlocking();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            exchange.getInputStream().transferTo(outputStream); // Reads the request body
            this.body = outputStream.toString(StandardCharsets.UTF_8);
        }
        return this.body;
    }

    @Override
    public void statusCode(int status) {
        this.exchange.setStatusCode(status);
    }

    @Override
    public void setHeader(String name, String value) {
        this.exchange.getResponseHeaders().put(new HttpString(name), value);
    }

    @Override
    public void write(byte[] bytes) {
        this.exchange.getResponseSender().send(ByteBuffer.wrap(bytes));
    }

    private void startBlocking() {
        if (!this.blockingStarted) {
            this.exchange.startBlocking();
            this.blockingStarted = true;
        }
    }
}
