package io.token;

import io.token.proto.common.token.TokenProtos.TransferTokenStatus;

/**
 * Thrown when a transfer token creation fails.
 */
public final class TransferTokenException extends RuntimeException {
    private final TransferTokenStatus status;

    public TransferTokenException(TransferTokenStatus status) {
        super("Failed to create token: " + status);
        this.status = status;
    }

    public TransferTokenStatus getStatus() {
        return status;
    }
}
