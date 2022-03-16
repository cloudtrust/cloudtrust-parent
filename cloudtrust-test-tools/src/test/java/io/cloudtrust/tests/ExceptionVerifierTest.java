package io.cloudtrust.tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ExceptionVerifierTest {
    public static class MyValidException extends Exception {
        private static final long serialVersionUID = -1505973559678938268L;

        public MyValidException(String message) {
            super(message);
        }

        public MyValidException(Throwable cause) {
            super(cause);
        }

        public MyValidException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class MyInvalidException extends Exception {
        private static final long serialVersionUID = 5479059760039628058L;

        public MyInvalidException(Throwable cause) {
            super(new Exception());
        }
    }

    @Test
    void validationSuccessTest() {
        ExceptionVerifier.verify(MyValidException.class);
    }

    @Test
    void validationFailureTest() {
        Assertions.assertThrows(AssertionError.class, () -> ExceptionVerifier.verify(MyInvalidException.class));
    }
}
