package io.cloudtrust.keycloak.test.http;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

import java.util.Collection;

public class HeaderHandlerImpl implements HeaderHandler {
    private final HttpServerExchange exchange;

    public HeaderHandlerImpl(HttpServerExchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public Collection<String> getHeaderNames() {
        return exchange.getRequestHeaders().getHeaderNames().stream().map(HttpString::toString).toList();
    }

    @Override
    public String getFirstHeader(String name) {
        return exchange.getRequestHeaders().getFirst(name);
    }

    @Override
    public Collection<String> getHeader(String name) {
        return exchange.getRequestHeaders().get(name);
    }
}
