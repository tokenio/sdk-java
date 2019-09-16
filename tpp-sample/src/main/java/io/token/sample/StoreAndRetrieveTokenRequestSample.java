package io.token.sample;

import static io.token.proto.common.token.TokenProtos.TokenRequestPayload.AccessBody.ResourceType.ACCOUNTS;
import static io.token.proto.common.token.TokenProtos.TokenRequestPayload.AccessBody.ResourceType.BALANCES;
import static io.token.util.Util.generateNonce;

import io.token.proto.common.alias.AliasProtos;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferDestination;
import io.token.tokenrequest.TokenRequest;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;

import java.util.List;

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
     * Stores a transfer token without setting Transfer Destinations.
     *
     * @param payee Payee Token member (the member requesting the transfer token be created)
     * @param setTransferDestinationsCallback callback url.
     * @return token request id
     */
    public static String storeTransferTokenRequest(
            Member payee,
            String setTransferDestinationsCallback) {

        TokenRequest request = TokenRequest.transferTokenRequestBuilder(100, "EUR")
                .setToMemberId(payee.memberId())
                .setDescription("Book purchase")
                // This TPP provided url gets called by Token after the user selects bank and
                // country on the Token web app.
                .setSetTransferDestinationsUrl(setTransferDestinationsCallback)
                // This TPP provided Redirect URL gets called after Token is ready
                // for redemption.
                .setRedirectUrl("https://tpp-sample.com/callback")
                .setFromAlias(AliasProtos.Alias.newBuilder()
                        .setValue("payer-alias@token.io") // user alias
                        .setType(AliasProtos.Alias.Type.EMAIL)
                        .build())
                .setBankId("iron") // bank ID
                .setCsrfToken(generateNonce()) // nonce for CSRF check
                .build();

        String requestId = payee.storeTokenRequestBlocking(request);

        return requestId;
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

    /**
     * Sets transfer destinations for a given token request.
     *
     * @param payee Payee Token member (the member requesting the transfer token be created)
     * @param requestId token request id
     * @param transferDestinations list of transfer destination
     */
    public static void setTokenRequestTransferDestinations(
            Member payee,
            String requestId,
            List<TransferDestination> transferDestinations) {

        payee.setTokenRequestTransferDestinationsBlocking(requestId, transferDestinations);
    }
}