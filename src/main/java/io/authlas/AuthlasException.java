package io.authlas;

/** Thrown when the authlas core rejects an input or a token. The message carries the core's status and error text. */
public class AuthlasException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AuthlasException(String message) {
        super(message);
    }
}
