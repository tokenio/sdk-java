package io.token.sample;

import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.tpp.Member;

/**
 * Cancels an access token.
 */
public final class CancelAccessTokenSample {
    /**
     * Cancels an access token.
     *
     * @param grantee grantee Token member
     * @param tokenId token ID to cancel
     * @return operation result
     */
    public static TokenOperationResult cancelAccessToken(Member grantee, String tokenId) {
        // Retrieve an access token to cancel.
        Token accessToken = grantee.getTokenBlocking(tokenId);

        // Cancel access token.
        return grantee.cancelTokenBlocking(accessToken);
    }
}
