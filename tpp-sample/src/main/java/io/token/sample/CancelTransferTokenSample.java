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
     * @param grantee grantee Token member
     * @param tokenId token ID to cancel
     * @return operation result
     */
    public static TokenOperationResult cancelTransferToken(Member grantee, String tokenId) {
        // Retrieve a transfer token to cancel.
        Token transferToken = grantee.getTokenBlocking(tokenId);

        // Cancel transfer token.
        return grantee.cancelTokenBlocking(transferToken);
    }
}
