package io.token.sample;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;

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
}
