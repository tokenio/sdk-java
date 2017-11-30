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
        for (Token token : grantor.getAccessTokens(null, 100)
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
     * @param oldToken token to replace
     * @return success or failure
     */
    public static TokenOperationResult replaceAccessToken(
            Member grantor,
            Alias granteeAlias,
            Token oldToken) {
        // Replace, but don't endorse the replacement
        // (replaceAndEndorseAccessToken is much safer
        // the "find" code doesn't see unendorsed tokens,
        // so if the unendorsed token needs replacing,
        // it perhaps can't be "found").
        TokenOperationResult status = grantor.replaceAccessToken(
                oldToken,
                AccessTokenBuilder
                        .fromPayload(oldToken.getPayload())
                        .forAllAccounts()
                        .forAllBalances()
                        .forAllAddresses());
        return status;
    }

    /**
     * Replaces and endorses a previously-created access token.
     *
     * @param grantor Token member granting access to her accounts
     * @param granteeAlias Token member alias acquiring information access
     * @param oldToken token to replace
     * @return success or failure
     */
    public static TokenOperationResult replaceAndEndorseAccessToken(
            Member grantor,
            Alias granteeAlias,
            Token oldToken) {
        // Replace old access token:
        TokenOperationResult status = grantor.replaceAndEndorseAccessToken(
                oldToken,
                AccessTokenBuilder
                        .fromPayload(oldToken.getPayload())
                        .forAllAccounts()
                        .forAllBalances()
                        .forAllAddresses());
        return status;
    }
}
