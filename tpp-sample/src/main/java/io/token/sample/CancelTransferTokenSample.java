package io.token.sample;

import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.tpp.Member;

/**
 * Cancels a transfer token.
 */
public final class CancelTransferTokenSample {
    /**
     * Cancels a transfer token.
     *
     * @param payee payee Token member
     * @param tokenId token ID to cancel
     * @return operation result
     */
    public static TokenOperationResult cancelTransferToken(Member payee, String tokenId) {
        // Retrieve a transfer token to cancel.
        Token transferToken = payee.getTokenBlocking(tokenId);

        // Cancel transfer token.
        return payee.cancelTokenBlocking(transferToken);
    }
}
