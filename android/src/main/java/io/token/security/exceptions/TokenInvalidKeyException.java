package io.token.security.exceptions;

/**
 * Thrown when the key or keys in the Android KeyStore are invalid. This can also happen when
 * the user changes their lockscreen passcode, or biometrics.
 */
public class TokenInvalidKeyException extends RuntimeException {
    private final Exception cause;

    public TokenInvalidKeyException(Exception cause) {
        super();
        this.cause = cause;
    }

    public Exception getCause() {
        return cause;
    }
}
