package io.token.sample;

import static io.token.TokenRequest.TokenRequestOptions.ALIAS;
import static io.token.TokenRequest.TokenRequestOptions.BANK_ID;
import static io.token.TokenRequest.TokenRequestOptions.REDIRECT_URL;

import io.token.AccessTokenBuilder;
import io.token.Member;
import io.token.TokenIO;
import io.token.TokenRequest;
import io.token.TokenRequestAndCustomization;
import io.token.TransferTokenBuilder;

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
        // Create a TransferTokenBuilder
        TransferTokenBuilder tokenBuilder =
                new TransferTokenBuilder(100.0, "EUR") // currency and amount
                        .setDescription("Book purchase") // optional description
                        .setToMemberId(payee.memberId()); // redeemer member id

        // Create a TokenRequest to be stored
        TokenRequest request = TokenRequest.newBuilder(tokenBuilder)
                .addOption(ALIAS, "payer-alias@token.io") // user alias
                .addOption(BANK_ID, "iron") // bank id
                .addOption(REDIRECT_URL, "https://token.io/callback") // callback url
                .build();

        // Store token request
        return payee.storeTokenRequest(request);
    }

    /**
     * Stores an access token request.
     *
     * @param grantee Grantee Token member (the member requesting the access token be created)
     * @return a token request id
     */
    public static String storeAccessTokenRequest(Member grantee) {
        // Create an AccessTokenBuilder
        AccessTokenBuilder tokenBuilder = AccessTokenBuilder.create(grantee.memberId()).forAll();

        // Create a TokenRequest to be stored
        TokenRequest request = TokenRequest.newBuilder(tokenBuilder)
                // Configure options for the TokenRequest
                .addOption(ALIAS, "user-alias@token.io")
                .addOption(BANK_ID, "iron")
                .addOption(REDIRECT_URL, "https://token.io/callback")
                .build();

        return grantee.storeTokenRequest(request);
    }

    /**
     * Retrieves a token request.
     *
     * @param tokenIO tokenIO instnace to use
     * @param requestId id of request to retrieve
     * @return token request and customization that was stored with the request id
     */
    public static TokenRequestAndCustomization retrieveTokenRequest(
            TokenIO tokenIO,
            String requestId) {
        return tokenIO.retrieveTokenRequest(requestId);
    }
}
