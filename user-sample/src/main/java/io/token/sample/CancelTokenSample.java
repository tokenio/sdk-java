package io.token.sample;

import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.user.Member;

/**
 * Cancels an access token.
 */
public final class CancelTokenSample {
    /**
     * Cancels an access token.
     *
     * @param grantor grantor Token member
     * @param tokenId token ID to cancel
     * @return operation result
     */
    public static TokenOperationResult cancelAccessToken(Member grantor, String tokenId) {
        // Retrieve an access token to cancel.
        Token accessToken = grantor.getTokenBlocking(tokenId);

        // Cancel access token.
        return grantor.cancelTokenBlocking(accessToken);
    }

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
