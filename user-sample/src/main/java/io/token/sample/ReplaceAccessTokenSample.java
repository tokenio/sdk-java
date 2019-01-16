package io.token.sample;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.user.AccessTokenBuilder;
import io.token.user.Member;
import io.token.user.TokenClient;

/**
 * Working with existing access tokens: finding and replacing.
 */
public final class ReplaceAccessTokenSample {
    /**
     * Finds a previously-created access token from grantor to grantee.
     *
     * @param tokenClient initialized SDK
     * @param grantor Token member granting access to her accounts
     * @param granteeAlias Token member alias acquiring information access
     * @return an access Token
     */
    public static Token findAccessToken(
            TokenClient tokenClient,
            Member grantor,
            Alias granteeAlias) {
        String granteeMemberId = tokenClient.getMemberIdBlocking(granteeAlias);
        return grantor.getActiveAccessTokenBlocking(granteeMemberId);
    }

    /**
     * Replaces a previously-created access token.
     *
     * @param grantor Token member granting access to her accounts
     * @param granteeAlias Token member alias acquiring information access
     * @param oldToken token to replace
     * @return success or failure
     */
    public static TokenOperationResult replaceAccessToken(
            Member grantor,
            Alias granteeAlias,
            Token oldToken) {
        String accountId = grantor.createAndLinkTestBankAccountBlocking(1000.0, "EUR")
                .id();

        // Replace the old access token
        Token newToken = grantor.replaceAccessTokenBlocking(
                oldToken,
                AccessTokenBuilder
                        .fromPayload(oldToken.getPayload())
                        .forAccount(accountId))
                .getToken();

        // Endorse the new access token
        TokenOperationResult status = grantor.endorseTokenBlocking(newToken, Key.Level.STANDARD);

        return status;
    }
}
