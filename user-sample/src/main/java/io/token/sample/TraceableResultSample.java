package io.token.sample;

import io.token.TokenClient;
import io.token.TraceableResult;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.user.Member;

public class TraceableResultSample {
    /**
     * Cancels an access token and wraps it with associated trace id .
     *
     * @param grantee grantee Token member
     * @param tokenId token ID to cancel
     * @return operation result
     */
    public static TraceableResult<TokenOperationResult> cancelAccessTokenAndReturnTraceId(
            Member grantee,
            String tokenId) {
        // Any call can be instead of cancelAccessToken call.

        // Retrieve an access token to cancel.
        Token accessToken = grantee.getTokenBlocking(tokenId);

        // Cancel access token.
        return TokenClient.trace(grantee.cancelToken(accessToken)).blockingSingle();
    }
}
