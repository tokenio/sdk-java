package io.token.sample;

import static io.token.util.Util.generateNonce;

import io.token.proto.common.alias.AliasProtos;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferDestination;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferDestination.DestinationCase;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferDestination.FasterPayments;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferDestination.Sepa;
import io.token.tokenrequest.TokenRequest;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;
import io.token.tpp.tokenrequest.TokenRequestTransferDestinationsCallbackParameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Sets transfer destination in the TokenRequest.
 */
public class SetTransferDestinationsSample {

    /**
     * Sets Transfer Destination after the call to storeTokenRequest.
     *
     * @param payee Payee Token member (the member requesting the transfer token be created)
     * @param tokenClient SDK client
     * @return token request id
     */
    public static String setTransferDestinations(
            Member payee,
            TokenClient tokenClient) {

        TokenRequest request = TokenRequest.transferTokenRequestBuilder(100, "EUR")
                .setToMemberId(payee.memberId())
                .setDescription("Book purchase")
                .setSetTransferDestinationsUrl("https://token.io/callback/transferDestinations")
                .setRedirectUrl("https://token.io/callback") // callback URL
                .setFromAlias(AliasProtos.Alias.newBuilder()
                        .setValue("payer-alias@token.io") // user alias
                        .setType(AliasProtos.Alias.Type.EMAIL)
                        .build())
                .setBankId("iron") // bank ID
                .setCsrfToken(generateNonce()) // nonce for CSRF check
                .build();

        String requestId = payee.storeTokenRequestBlocking(request);

        String setTransferDestinationsCallback = "https://token.io/callback/transferDestinations"
                + "?supportedTransferDestinationType=FASTER_PAYMENTS&"
                + "supportedTransferDestinationType=SEPA&bankName=Iron&country=UK";

        triggerCallback(payee, tokenClient, requestId, setTransferDestinationsCallback);

        return requestId;
    }

    private static void triggerCallback(
            Member payee,
            TokenClient tokenClient,
            String requestId,
            String setTransferDestinationsUrl) {

        TokenRequestTransferDestinationsCallbackParameters params =
                tokenClient.parseSetTransferDestinationsUrl(setTransferDestinationsUrl);

        List<TransferDestination> transferDestinations = new ArrayList<>();
        if (params.getSupportedTransferDestinationTypes()
                .contains(DestinationCase.FASTER_PAYMENTS)) {
            TransferDestination destination = TransferDestination.newBuilder()
                    .setFasterPayments(FasterPayments
                            .newBuilder()
                            .setSortCode(generateNonce())
                            .setAccountNumber(generateNonce())
                            .build())
                    .build();
            transferDestinations.add(destination);
        } else {
            transferDestinations.add(TransferDestination
                    .newBuilder()
                    .setSepa(Sepa
                            .newBuilder()
                            .setBic(generateNonce())
                            .setIban(generateNonce())
                            .build())
                    .build());
        }

        payee.setTokenRequestTransferDestinationsBlocking(requestId, transferDestinations);
    }
}
