package io.token.sample;

import static io.token.proto.common.token.TokenProtos.TokenRequestPayload.AccessBody.ResourceType.ACCOUNTS;
import static io.token.proto.common.token.TokenProtos.TokenRequestPayload.AccessBody.ResourceType.BALANCES;
import static io.token.util.Util.generateNonce;

import io.token.proto.common.account.AccountProtos.AccountIdentifier;
import io.token.proto.common.alias.AliasProtos;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferDestination;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferDestination.DestinationCase;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferEndpoint;
import io.token.tokenrequest.TokenRequest;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;
import io.token.tpp.tokenrequest.TokenRequestTransferDestinationsCallbackParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
                .setRefId(UUID.randomUUID().toString().replace("-", ""))
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
     * Stores a transfer token request with a preselected source account.
     *
     * @param payee Payee Token member (the member requesting the transfer token be created)
     * @param source the source account
     * @return a token request id
     */
    public static String storeTransferTokenRequestWithSource(
            Member payee,
            TransferEndpoint source) {
        // Create token request to be stored
        TokenRequest request = TokenRequest.transferTokenRequestBuilder(100., "EUR")
                .setRefId(UUID.randomUUID().toString().replace("-", ""))
                .setToMemberId(payee.memberId())
                .setDescription("Book purchase") // optional description
                .setRedirectUrl("https://token.io/callback") // callback URL
                .setFromAlias(Alias.newBuilder()
                        .setValue("payer-alias@token.io") // user alias
                        .setType(Alias.Type.EMAIL)
                        .build())
                .setSource(source)
                .setCsrfToken(generateNonce()) // nonce for CSRF check
                .build();

        // Store token request
        return payee.storeTokenRequestBlocking(request);
    }

    /**
     * Stores a transfer token without setting Transfer Destinations and instead providing
     * a callback URL.
     *
     * @param payee Payee Token member (the member requesting the transfer token be created)
     * @param setTransferDestinationsCallback callback url.
     * @return token request id
     */
    public static String storeTransferTokenRequestWithDestinationsCallback(
            Member payee,
            String setTransferDestinationsCallback) {

        TokenRequest tokenRequest = TokenRequest.transferTokenRequestBuilder(250, "EUR")
                .setRefId(UUID.randomUUID().toString().replace("-", ""))
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

        String requestId = payee.storeTokenRequestBlocking(tokenRequest);

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
                .setRefId(UUID.randomUUID().toString().replace("-", ""))
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
     * Stores an access token request with a preselected source account (for funds confirmation).
     *
     * @param grantee Token member requesting the access token be created
     * @param source source account
     * @param bankId source bank ID
     * @return a token request id
     */
    public static String storeAccessTokenRequestWithSource(
            Member grantee,
            AccountIdentifier source,
            String bankId) {
        // Create token request to be stored
        TokenRequest request = TokenRequest.fundsConfirmationRequestBuilder(bankId, source)
                .setRefId(UUID.randomUUID().toString().replace("-", ""))
                .setToMemberId(grantee.memberId())
                .setRedirectUrl("https://token.io/callback") // callback URL
                .setFromAlias(Alias.newBuilder()
                        .setValue("grantor-alias@token.io") // user alias
                        .setType(Alias.Type.EMAIL)
                        .build())
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
     * @param tokenClient Token SDK client
     * @param setTransferDestinationsCallback callback url
     */
    public static void setTokenRequestTransferDestinations(
            Member payee,
            String requestId,
            TokenClient tokenClient,
            String setTransferDestinationsCallback) {

        TokenRequestTransferDestinationsCallbackParameters params =
                tokenClient.parseSetTransferDestinationsUrl(setTransferDestinationsCallback);

        List<TransferDestination> transferDestinations = new ArrayList<>();
        if (params.getSupportedTransferDestinationTypes()
                .contains(DestinationCase.FASTER_PAYMENTS)) {
            TransferDestination destination = TransferDestination
                    .newBuilder()
                    .setFasterPayments(TransferDestination.FasterPayments
                            .newBuilder()
                            .setSortCode(generateNonce())
                            .setAccountNumber(generateNonce())
                            .build())
                    .build();
            transferDestinations.add(destination);
        } else {
            transferDestinations.add(TransferDestination
                    .newBuilder()
                    .setSepa(TransferDestination.Sepa
                            .newBuilder()
                            .setBic(generateNonce())
                            .setIban(generateNonce())
                            .build())
                    .build());
        }
        payee.setTokenRequestTransferDestinationsBlocking(requestId, transferDestinations);
    }
}
