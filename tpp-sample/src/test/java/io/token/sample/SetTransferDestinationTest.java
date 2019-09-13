package io.token.sample;

import static io.token.sample.SetTransferDestinationsSample.setTransferDestinations;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.token.tokenrequest.TokenRequest;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;

import org.junit.Test;

public class SetTransferDestinationTest {

    @Test
    public void setTransferDestinationsTest() {
        try (TokenClient tokenClient = createClient()) {
            Member payee = tokenClient.createMemberBlocking(randomAlias());
            String requestId = setTransferDestinations(payee, tokenClient);
            TokenRequest request = tokenClient.retrieveTokenRequestBlocking(requestId);
            assertThat(request).isNotNull();
            assertThat(request.getTokenRequestPayload().getTransferBody()).isNotNull();
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
