package io.token.sample;

import io.token.AccessTokenBuilder;
import io.token.Member;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;

import java.util.Optional;

/**
 * Creates an information access token and endorses it to a grantee.
 */
public final class ReplaceAccessTokenSample {
    /**
     * Creates an information access token to allow a grantee to see all bank accounts of a grantor.
     *
     * @param grantor Token member granting access to her accounts
     * @param granteeUsername Token member username acquiring information access
     * @return an access Token
     */
    public static Optional<Token> findAccessToken(Member grantor, String granteeUsername) {
        for (Token token : grantor.getAccessTokens("0", 100).getList()) {
            if (token.getPayload().getTo().getUsername().equals(granteeUsername)) {
                return Optional.of(token);
            }
        }
        return Optional.empty();
    }

    /**
     * Replaces a previously-created access token.
     *
     * @param grantor Token member granting access to her accounts
     * @param granteeUsername Token member username acquiring information access
     * @return success or failure
     */
    public static TokenOperationResult replaceAccessToken(Member grantor, String granteeUsername) {
        Token oldToken = findAccessToken(grantor, granteeUsername).get();
        TokenOperationResult status = grantor.replaceAccessToken(oldToken, AccessTokenBuilder
                .fromPayload(oldToken.getPayload())
                .forAllAddresses()
                .forAllTransactions());
        return status;
    }

    /**
     * Replaces and endorses a previously-created access token.
     *
     * @param grantor Token member granting access to her accounts
     * @param granteeUsername Token member username acquiring information access
     * @return success or failure
     */
    public static TokenOperationResult replaceAndEndorseAccessToken(
            Member grantor,
            String granteeUsername) {
        Token oldToken = findAccessToken(grantor, granteeUsername).get();
        TokenOperationResult status = grantor.replaceAccessToken(oldToken, AccessTokenBuilder
                .fromPayload(oldToken.getPayload())
                .forAllBalances()
                .forAllTransactions());
        return status;
    }
}
