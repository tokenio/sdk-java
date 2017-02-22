package io.token.exceptions;

/**
 * Thrown when the Token SDK version is no longer supported by the server. Any Token SDK callers
 * are required to update the Token SDK to the latest version to continue.
 */
public class VersionMismatchException extends RuntimeException {
    public VersionMismatchException(String message) {
        super(message);
    }
}
