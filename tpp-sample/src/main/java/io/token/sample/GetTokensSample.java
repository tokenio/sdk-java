package io.token.sample;

import io.token.proto.PagedList;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.proto.common.token.TokenProtos.TokenSignature;
import io.token.tpp.Member;

import java.util.List;

public class GetTokensSample {
    /**
     * Gets a token by ID.
     *
     * @param member member represented by the token (payer/payee/grantor/grantee)
     * @param tokenId token ID
     * @return token
     */
    public static Token getToken(Member member, String tokenId) {
        Token token = member.getTokenBlocking(tokenId);

        // get token payload
        TokenPayload payload = token.getPayload();

        // get signatures
        List<TokenSignature> signatures = token.getPayloadSignaturesList();

        return token;
    }

    /**
     * Gets a list of transfer tokens associated with a member.
     *
     * @param member member
     * @return paged list of transfer tokens
     */
    public static PagedList<Token, String> getTransferTokens(Member member) {
        // last 10 tokens and offset that can be used to get the next 10
        PagedList<Token, String> pagedList = member.getTransferTokensBlocking("", 10);

        return pagedList;
    }

    /**
     * Gets a list of access tokens associated with the member.
     *
     * @param member member
     * @return paged list of access tokens
     */
    public static PagedList<Token, String> getAccessTokens(Member member) {
        // last 10 tokens and offset that can be used to get the next 10
        PagedList<Token, String> pagedList = member.getAccessTokensBlocking("", 10);

        return pagedList;
    }
}
