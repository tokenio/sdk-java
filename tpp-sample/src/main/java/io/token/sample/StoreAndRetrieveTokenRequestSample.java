package io.token.sample;

import static io.token.proto.common.token.TokenProtos.TokenRequestPayload.AccessBody.ResourceType.ACCOUNTS;
import static io.token.proto.common.token.TokenProtos.TokenRequestPayload.AccessBody.ResourceType.BALANCES;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos.TokenMember;
import io.token.proto.common.token.TokenProtos.TokenRequestPayload;
import io.token.proto.common.token.TokenProtos.TokenRequestPayload.AccessBody;
import io.token.proto.common.token.TokenProtos.TokenRequestPayload.TransferBody;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;
import io.token.tpp.tokenrequest.TokenRequest;

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
        // Construct payload
        TokenRequestPayload payload = TokenRequestPayload.newBuilder()
                .setTo(TokenMember.newBuilder()
                        .setId(payee.memberId())
                        .build())
                .setTransferBody(TransferBody.newBuilder()
                        .setAmount("100.00") // amount
                        .setCurrency("EUR") // currency
                        .build())
                .setDescription("Book purchase") // optional description
                .setRedirectUrl("https://token.io/callback") // callback URL
                .build();

        // Create token request to be stored
        TokenRequest request = TokenRequest.newBuilder(payload)
                .setFrom(TokenMember.newBuilder()
                        .setAlias(Alias.newBuilder()
                                .setValue("payer-alias@token.io") // user alias
                                .setType(Alias.Type.EMAIL)
                                .build())
                        .build())
                .setBankId("iron") // bank ID
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
        // Construct payload
        TokenRequestPayload payload = TokenRequestPayload.newBuilder()
                .setTo(TokenMember.newBuilder()
                        .setId(grantee.memberId())
                        .build())
                .setAccessBody(AccessBody.newBuilder()
                        .addType(ACCOUNTS) // request access to basic account info
                        .addType(BALANCES) // request access to account balances
                        .build())
                .setRedirectUrl("https://token.io/callback") // callback URL
                .build();

        // Create token request to be stored
        TokenRequest request = TokenRequest.newBuilder(payload)
                .setFrom(TokenMember.newBuilder()
                        .setAlias(Alias.newBuilder()
                                .setValue("grantor-alias@token.io") // user alias
                                .setType(Alias.Type.EMAIL)
                                .build())
                        .build())
                .setBankId("iron") // bank ID
                .build();

        return grantee.storeTokenRequestBlocking(request);
    }

    /**
     * Retrieves a token request.
     *
     * @param tokenIO tokenIO instance to use
     * @param requestId id of request to retrieve
     * @return token request that was stored with the request id
     */
    public static TokenRequest retrieveTokenRequest(
            TokenClient tokenIO,
            String requestId) {
        return tokenIO.retrieveTokenRequestBlocking(requestId);
    }
}