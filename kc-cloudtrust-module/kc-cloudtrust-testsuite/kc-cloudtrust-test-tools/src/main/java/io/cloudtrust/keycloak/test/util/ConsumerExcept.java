package io.cloudtrust.keycloak.test.util;

public interface ConsumerExcept<T, E extends Throwable> {
    void accept(T param) throws E;
}
