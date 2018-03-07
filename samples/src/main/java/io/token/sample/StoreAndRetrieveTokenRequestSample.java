package io.token.sample;

import static io.token.TokenRequest.TokenRequestOptions.ALIAS;
import static io.token.TokenRequest.TokenRequestOptions.BANKID;
import static io.token.TokenRequest.TokenRequestOptions.REDIRECT_URL;

import io.token.AccessTokenBuilder;
import io.token.Member;
import io.token.TokenIO;
import io.token.TokenRequest;
import io.token.TransferTokenBuilder;

/**
 * Stores and retrieves a token request
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
        TokenRequest request = TokenRequest.create(tokenBuilder)
                .setOption(ALIAS, "payerAlias@token.io") // user alias
                .setOption(BANKID, "iron") // bank id
                .setOption(REDIRECT_URL, "https://token.io/callback"); // callback url

        // Store token request
        return payee.storeTokenRequest(request);
    }

    /**
     * Stores an access token request.
     *
     * @param payee Payee Token member (the member requesting the transfer token be created)
     * @return a token request id
     */
    public static String storeAccessTokenRequest(Member payee) {
        // Create an AccessTokenBuilder
        AccessTokenBuilder tokenBuilder = AccessTokenBuilder.create(payee.memberId()).forAll();

        // Create a TokenRequest to be stored
        TokenRequest request = TokenRequest.create(tokenBuilder)
                // Configure options for the TokenRequest
                .setOption(ALIAS, "userAlias@token.io")
                .setOption(BANKID, "iron")
                .setOption(REDIRECT_URL, "https://token.io/callback");

        return payee.storeTokenRequest(request);
    }

    /**
     * Retrieves a token request.
     *
     * @param tokenIO tokenIO instnace to use
     * @param requestId id of request to retrieve
     * @return a token request id
     */
    public static TokenRequest retrieveTokenRequest(
            TokenIO tokenIO,
            String requestId) {
        return tokenIO.retrieveTokenRequest(requestId);
    }
}
