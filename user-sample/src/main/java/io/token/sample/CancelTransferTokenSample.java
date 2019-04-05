package io.token.sample;

import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.user.Member;

/**
 * Cancels a transfer token.
 */
public final class CancelTransferTokenSample {
    /**
     * Cancels a transfer token.
     *
     * @param payer payer Token member
     * @param tokenId token ID to cancel
     * @return operation result
     */
    public static TokenOperationResult cancelTransferToken(Member payer, String tokenId) {
        // Retrieve a transfer token to cancel.
        Token transferToken = payer.getTokenBlocking(tokenId);

        // Cancel transfer token.
        return payer.cancelTokenBlocking(transferToken);
    }
}
