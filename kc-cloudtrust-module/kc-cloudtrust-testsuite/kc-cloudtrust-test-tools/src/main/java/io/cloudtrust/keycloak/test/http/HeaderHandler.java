package io.cloudtrust.keycloak.test.http;

import java.util.Collection;

public interface HeaderHandler {
    Collection<String> getHeaderNames();
    String getFirstHeader(String name);
    Collection<String> getHeader(String name);
}
