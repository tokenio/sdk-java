package io.token.sample;

import io.token.AccessTokenBuilder;
import io.token.Member;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos.Token;

/**
 * Creates an information access token and endorses it to a grantee.
 */
public final class CreateAndEndorseAccessTokenSample {
    /**
     * Creates an information access token to allow a grantee to see all bank accounts of a grantor.
     *
     * @param grantor Token member granting access to her accounts
     * @param granteeAlias Token member alias acquiring information access
     * @return an access Token
     */
    public static Token createAccessToken(Member grantor, Alias granteeAlias) {
        // Create an access token for the grantee to access bank account names of the grantor.
        Token accessToken = grantor.createAccessToken(AccessTokenBuilder
                .create(granteeAlias)
                .forAllAccounts());

        // Grantor endorses a token to a grantee by signing it with her secure private key.
        accessToken = grantor.endorseToken(accessToken, Key.Level.STANDARD).getToken();

        return accessToken;
    }
}
