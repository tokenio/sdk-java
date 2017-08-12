package io.token.sample;

import io.token.AccessTokenBuilder;
import io.token.Member;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;

import java.util.Optional;

/**
 * Working with existing access tokens: finding and replacing.
 */
public final class ReplaceAccessTokenSample {
    /**
     * Finds a previously-created access token from grantor to grantee.
     *
     * @param grantor Token member granting access to her accounts
     * @param granteeAlias Token member alias acquiring information access
     * @return an access Token
     */
    public static Optional<Token> findAccessToken(Member grantor, Alias granteeAlias) {
        for (Token token : grantor.getAccessTokens("0", 100)
                .getList()) {
            Alias toAlias = token.getPayload().getTo().getAlias();
            if (toAlias.equals(granteeAlias)) {
                return Optional.of(token);
            }
        }
        return Optional.empty();
    }

    /**
     * Replaces a previously-created access token.
     *
     * @param grantor Token member granting access to her accounts
     * @param granteeAlias Token member alias acquiring information access
     * @return success or failure
     */
    public static TokenOperationResult replaceAccessToken(Member grantor, Alias granteeAlias) {
        // Replace, but don't endorse the replacement:
        Token oldToken = findAccessToken(grantor, granteeAlias).get();
        TokenOperationResult status = grantor.replaceAccessToken(
                oldToken,
                AccessTokenBuilder
                        .fromPayload(oldToken.getPayload())
                        .forAllAddresses()
                        .forAllTransactions());
        return status;
    }

    /**
     * Replaces and endorses a previously-created access token.
     *
     * @param grantor Token member granting access to her accounts
     * @param granteeAlias Token member alias acquiring information access
     * @return success or failure
     */
    public static TokenOperationResult replaceAndEndorseAccessToken(
            Member grantor,
            Alias granteeAlias) {
        // Replace old access token:
        Token oldToken = findAccessToken(grantor, granteeAlias).get();
        TokenOperationResult status = grantor.replaceAccessToken(
                oldToken,
                AccessTokenBuilder
                        .fromPayload(oldToken.getPayload())
                        .forAllBalances()
                        .forAllTransactions());
        return status;
    }
}
