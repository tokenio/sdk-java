package io.token.sample;

import static io.token.sample.StoreAndRetrieveTokenRequestSample.storeAccessTokenRequest;
import static io.token.sample.StoreAndRetrieveTokenRequestSample.storeTransferTokenRequest;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.token.tpp.Member;
import io.token.tpp.TokenClient;
import io.token.tpp.tokenrequest.TokenRequest;

import org.junit.Test;

/**
 * Sample to show how to store and retrieve token requests.
 */
public class StoreAndRetrieveTokenRequestSampleTest {
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
}