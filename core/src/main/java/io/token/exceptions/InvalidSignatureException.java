package io.token.exceptions;

/**
 * Thrown when the signature included in a request fails validation.
 */
public class InvalidSignatureException extends RuntimeException {
    public InvalidSignatureException(String message) {
        super(message);
    }
}
