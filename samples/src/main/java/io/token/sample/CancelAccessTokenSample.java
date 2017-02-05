package io.token.sample;

import io.token.Member;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;

/**
 * Cancels an access token.
 */
public final class CancelAccessTokenSample {
    /**
     * Cancels an access token.
     *
     * @param grantor grantor Token member
     * @param tokenId token ID to cancel
     * @return operation result
     */
    public static TokenOperationResult cancelAccessToken(Member grantor, String tokenId) {
        // Retrieve an access token to cancel.
        Token accessToken = grantor.getToken(tokenId);

        // Cancel access token.
        return grantor.cancelToken(accessToken);
    }
}
