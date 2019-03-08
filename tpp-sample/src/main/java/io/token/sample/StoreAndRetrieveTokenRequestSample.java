package io.token.sample;

import static io.token.proto.common.token.TokenProtos.TokenRequestPayload.AccessBody.ResourceType.ACCOUNTS;
import static io.token.proto.common.token.TokenProtos.TokenRequestPayload.AccessBody.ResourceType.BALANCES;
import static io.token.util.Util.generateNonce;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.tokenrequest.TokenRequest;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;
import io.token.tpp.util.Util;

/**
 * Stores and retrieves a token request.
 */
public final class StoreAndRetrieveTokenRequestSample {
    /**
     * Stores a transfer token request.
     *
     * @param payee Payee Token member (the member requesting the transfer token be created)
     * @return a token request id
     */
    public static String storeTransferTokenRequest(Member payee) {
        // Create token request to be stored
        TokenRequest request = TokenRequest.transferTokenRequestBuilder(100., "EUR")
                .setToMemberId(payee.memberId())
                .setDescription("Book purchase") // optional description
                .setRedirectUrl("https://token.io/callback") // callback URL
                .setFromAlias(Alias.newBuilder()
                                .setValue("payer-alias@token.io") // user alias
                                .setType(Alias.Type.EMAIL)
                                .build())
                .setBankId("iron") // bank ID
                .setCsrfToken(generateNonce()) // nonce for CSRF check
                .build();

        // Store token request
        return payee.storeTokenRequestBlocking(request);
    }

    /**
     * Stores an access token request.
     *
     * @param grantee Token member requesting the access token be created
     * @return a token request id
     */
    public static String storeAccessTokenRequest(Member grantee) {
        // Create token request to be stored
        TokenRequest request = TokenRequest.accessTokenRequestBuilder(ACCOUNTS, BALANCES)
                .setToMemberId(grantee.memberId())
                .setRedirectUrl("https://token.io/callback") // callback URL
                .setFromAlias(Alias.newBuilder()
                                .setValue("grantor-alias@token.io") // user alias
                                .setType(Alias.Type.EMAIL)
                                .build())
                .setBankId("iron") // bank ID
                .setCsrfToken(generateNonce()) // nonce for CSRF check
                .build();

        return grantee.storeTokenRequestBlocking(request);
    }

    /**
     * Retrieves a token request.
     *
     * @param tokenClient tokenIO instance to use
     * @param requestId id of request to retrieve
     * @return token request that was stored with the request id
     */
    public static TokenRequest retrieveTokenRequest(
            TokenClient tokenClient,
            String requestId) {
        return tokenClient.retrieveTokenRequestBlocking(requestId);
    }
}