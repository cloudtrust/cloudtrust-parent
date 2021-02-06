package io.cloudtrust.exception;

import io.cloudtrust.tests.ExceptionVerifier;
import org.junit.Test;

public class ExceptionTest {
    @Test
    public void cloudtrustExceptionCoverage() {
        ExceptionVerifier.verify(CloudtrustException.class);
    }

    @Test
    public void cloudtrustRuntimeExceptionCoverage() {
        ExceptionVerifier.verify(CloudtrustRuntimeException.class);
    }
}
