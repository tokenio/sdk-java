package io.token.exceptions;

public class SecureHardwareKeystoreRequiredException extends RuntimeException {
    public SecureHardwareKeystoreRequiredException() {
        super("Secure hardware keystore is required(e.g., Trusted Execution Environment (TEE) or Secure Element (SE)).");
    }
}