package io.cloudtrust.exception;

public class CloudtrustException extends Exception {
    private static final long serialVersionUID = 1337533331050583350L;

    public CloudtrustException() {
    }

    public CloudtrustException(String message) {
        super(message);
    }

    public CloudtrustException(Throwable cause) {
        super(cause);
    }
}
