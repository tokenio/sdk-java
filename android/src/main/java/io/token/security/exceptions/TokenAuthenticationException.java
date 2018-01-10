package io.token.security.exceptions;

import java.security.Signature;

/**
 * Thrown when additional user authentication is required to use the Token API. This includes a
 * Signature object, the signature that would have been created, had the operation succeeed.
 */
public class TokenAuthenticationException extends RuntimeException {
    private final Signature signature;

    public TokenAuthenticationException(Signature signature) {
        super();
        this.signature = signature;
    }

    public Signature getSignature() {
        return signature;
    }
}
