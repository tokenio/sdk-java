package io.token.sample;

import static io.token.sample.StoreAndRetrieveTokenRequestSample.storeAccessTokenRequest;
import static io.token.sample.StoreAndRetrieveTokenRequestSample.storeTransferTokenRequest;
import static io.token.sample.TestUtil.createClient;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.token.Member;
import io.token.TokenIO;
import io.token.TokenRequest;

import org.junit.Test;

/**
 * Sample to show how to store and retrieve token requests.
 */
public class StoreAndRetrieveTokenRequestSampleTest {
    @Test
    public void storeAndRetrieveTransferTokenTest() {
        try (TokenIO tokenIO = createClient()) {
            Member payee = tokenIO.createMember();
            String requestId = storeTransferTokenRequest(payee);
            TokenRequest request = tokenIO.retrieveTokenRequest(requestId).getTokenRequest();
            assertThat(request).isNotNull();
        }
    }

    @Test
    public void storeAndRetrieveAccessTokenTest() {
        try (TokenIO tokenIO = createClient()) {
            Member grantee = tokenIO.createMember();
            String requestId = storeAccessTokenRequest(grantee);
            TokenRequest request = tokenIO.retrieveTokenRequest(requestId).getTokenRequest();
            assertThat(request).isNotNull();
        }
    }
}
