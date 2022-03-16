package io.cloudtrust.keycloak.test.http;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;

import java.util.Collection;
import java.util.stream.Collectors;

public class HeaderHandlerImpl implements HeaderHandler {
    private final HeaderMap headers;

    public HeaderHandlerImpl(HttpServerExchange exchange) {
        this.headers = exchange.getRequestHeaders();
    }

    @Override
    public Collection<String> getHeaderNames() {
        return headers.getHeaderNames().stream().map(HttpString::toString).collect(Collectors.toList());
    }

    @Override
    public String getFirstHeader(String name) {
        return headers.get(name, 0);
    }

    @Override
    public Collection<String> getHeader(String name) {
        return headers.get(name);
    }
}
