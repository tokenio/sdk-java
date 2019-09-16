package io.token.sample;

import static io.token.sample.StoreAndRetrieveTokenRequestSample.setTokenRequestTransferDestinations;
import static io.token.sample.StoreAndRetrieveTokenRequestSample.storeAccessTokenRequest;
import static io.token.sample.StoreAndRetrieveTokenRequestSample.storeTransferTokenRequest;
import static io.token.sample.StoreAndRetrieveTokenRequestSample.storeTransferTokenRequestWithDestinationsCallback;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.token.tokenrequest.TokenRequest;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;

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
            String requestId = storeTransferTokenRequestWithDestinationsCallback(
                    payee,
                    setTransferDestinationsUrl);
            setTokenRequestTransferDestinations(
                    payee,
                    requestId,
                    tokenClient,
                    setTransferDestinationsCallback);
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
