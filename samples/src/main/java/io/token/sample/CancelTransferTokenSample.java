package io.token.sample;

import io.token.Member;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;

/**
 * Cancels a transfer token.
 */
public final class CancelTransferTokenSample {
    /**
     * Cancels a transfer token.
     *
     * @param grantor grantor Token member
     * @param tokenId token ID to cancel
     * @return operation result
     */
    public static TokenOperationResult cancelTransferToken(Member grantor, String tokenId) {
        // Retrieve a transfer token to cancel.
        Token transferToken = grantor.getToken(tokenId);

        // Cancel transfer token.
        return grantor.cancelToken(transferToken);
    }
}
