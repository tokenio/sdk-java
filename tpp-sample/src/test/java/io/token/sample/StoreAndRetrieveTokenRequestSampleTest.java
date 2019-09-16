package io.token.sample;

import static io.token.sample.StoreAndRetrieveTokenRequestSample.setTokenRequestTransferDestinations;
import static io.token.sample.StoreAndRetrieveTokenRequestSample.storeAccessTokenRequest;
import static io.token.sample.StoreAndRetrieveTokenRequestSample.storeTransferTokenRequest;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.randomAlias;
import static io.token.util.Util.generateNonce;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferDestination;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferDestination.DestinationCase;
import io.token.tokenrequest.TokenRequest;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;
import io.token.tpp.tokenrequest.TokenRequestTransferDestinationsCallbackParameters;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Sample to show how to store and retrieve token requests.
 */
public class StoreAndRetrieveTokenRequestSampleTest {
    private static String setTransferDestinationsUrl = "https://tpp-sample.com/callback/"
            + "transferDestinations";
    private static String setTransferDestinationsCallback = "https://tpp-sample.com/callback/"
            + "transferDestinations?supportedTransferDestinationType=FASTER_PAYMENTS&"
            + "supportedTransferDestinationType=SEPA&bankName=Iron&country=UK";

    @Test
    public void storeAndRetrieveTransferTokenTest() {
        try (TokenClient tokenClient = createClient()) {
            Member payee = tokenClient.createMemberBlocking(randomAlias());
            String requestId = storeTransferTokenRequest(payee);
            TokenRequest request = tokenClient.retrieveTokenRequestBlocking(requestId);
            assertThat(request).isNotNull();
        }
    }

    @Test
    public void storeAndRetrieveAccessTokenTest() {
        try (TokenClient tokenClient = createClient()) {
            Member grantee = tokenClient.createMemberBlocking(randomAlias());
            String requestId = storeAccessTokenRequest(grantee);
            TokenRequest request = tokenClient.retrieveTokenRequestBlocking(requestId);
            assertThat(request).isNotNull();
        }
    }

    @Test
    public void storeTokenRequestAndSetTransferDestinationsTest() {
        try (TokenClient tokenClient = createClient()) {
            Member payee = tokenClient.createMemberBlocking(randomAlias());
            String requestId = storeTransferTokenRequest(payee, setTransferDestinationsUrl);

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

            setTokenRequestTransferDestinations(payee, requestId, transferDestinations);
            TokenRequest request = tokenClient.retrieveTokenRequestBlocking(requestId);
            assertThat(request).isNotNull();
            assertThat(request
                    .getTokenRequestPayload()
                    .getTransferBody()
                    .getInstructions()
                    .getTransferDestinationsCount()).isNotEqualTo(0);
            assertThat(request
                    .getTokenRequestPayload()
                    .getTransferBody()
                    .getInstructions()
                    .getTransferDestinationsList().get(0).hasFasterPayments()).isTrue();
        }
    }
}
