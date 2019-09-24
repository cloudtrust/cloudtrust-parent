package io.cloudtrust.exception;

public class CloudtrustRuntimeException extends RuntimeException {
    private static final long serialVersionUID = 8438754018850435511L;

    public CloudtrustRuntimeException() {
    }

    public CloudtrustRuntimeException(String message) {
        super(message);
    }

    public CloudtrustRuntimeException(Throwable cause) {
        super(cause);
    }

    public CloudtrustRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
