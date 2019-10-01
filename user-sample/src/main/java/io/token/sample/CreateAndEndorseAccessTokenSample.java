package io.token.sample;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.user.AccessTokenBuilder;
import io.token.user.Member;

/**
 * Creates an information access token and endorses it to a grantee.
 */
public final class CreateAndEndorseAccessTokenSample {
    /**
     * Creates an information access token to allow a grantee to see the balance
     * of one of the grantor's accounts.
     *
     * @param grantor Token member granting access to her account
     * @param accountId ID of account to grant access to.
     * @param granteeAlias Token member alias acquiring information access
     * @return an access Token
     */
    public static Token createBalanceAccessToken(
            Member grantor,
            String accountId,
            Alias granteeAlias) {
        // Create an access token for the grantee to access bank
        // account names of the grantor.
        Token accessToken = grantor.createAccessTokenBlocking(
                AccessTokenBuilder
                        .create(granteeAlias)
                        .forAccount(accountId)
                        .forAccountBalances(accountId));

        // Grantor endorses a token to a grantee by signing it
        // with her secure private key.
        accessToken = grantor.endorseTokenBlocking(
                accessToken,
                Key.Level.STANDARD).getToken();

        return accessToken;
    }

    /**
     * Creates an information access token to allow a grantee to see the transaction history
     * of one of the grantor's accounts.
     *
     * @param grantor Token member granting access to her account
     * @param accountId ID of account to grant access to.
     * @param granteeAlias Token member alias acquiring information access
     * @return an access Token
     */
    public static Token createTransactionsAccessToken(
            Member grantor,
            String accountId,
            Alias granteeAlias) {
        // Create an access token for the grantee to access bank
        // account names of the grantor.
        Token accessToken = grantor.createAccessTokenBlocking(
                AccessTokenBuilder
                        .create(granteeAlias)
                        .forAccount(accountId)
                        .forAccountTransactions(accountId));

        // Grantor endorses a token to a grantee by signing it
        // with her secure private key.
        accessToken = grantor.endorseTokenBlocking(
                accessToken,
                Key.Level.STANDARD).getToken();

        return accessToken;
    }

    /**
     * Creates an information access token to allow a grantee to see the standing orders
     * of one of the grantor's accounts.
     *
     * @param grantor Token member granting access to her account
     * @param accountId ID of account to grant access to.
     * @param granteeAlias Token member alias acquiring information access
     * @return an access Token
     */
    public static Token createStandingOrdersAccessToken(
            Member grantor,
            String accountId,
            Alias granteeAlias) {
        // Create an access token for the grantee to access bank
        // account names of the grantor.
        Token accessToken = grantor.createAccessTokenBlocking(
                AccessTokenBuilder
                        .create(granteeAlias)
                        .forAccount(accountId)
                        .forAccountStandingOrders(accountId));

        // Grantor endorses a token to a grantee by signing it
        // with her secure private key.
        accessToken = grantor.endorseTokenBlocking(
                accessToken,
                Key.Level.STANDARD).getToken();

        return accessToken;
    }
}
