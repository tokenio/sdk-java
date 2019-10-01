package io.token.sample;

import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.sample.CreateTransferTokenSample.createTransferToken;
import static io.token.sample.CreateTransferTokenSample.createTransferTokenScheduled;
import static io.token.sample.CreateTransferTokenSample.createTransferTokenToDestination;
import static io.token.sample.CreateTransferTokenSample.createTransferTokenWithOtherOptions;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createMemberAndLinkAccounts;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.user.Member;
import io.token.user.TokenClient;

import org.junit.Test;

public class CreateTransferTokenSampleTest {
    @Test
    public void createPaymentTokenTest() {
        try (TokenClient tokenClient = createClient()) {
            Member payer = createMemberAndLinkAccounts(tokenClient);
            Alias payeeAlias = randomAlias();
            Member payee = tokenClient.createMemberBlocking(payeeAlias);
            Token token = createTransferToken(payer, payeeAlias, LOW);
            assertThat(token).isNotNull();
        }
    }

    @Test
    public void createPaymentTokenWithOtherOptionsTest() {
        try (TokenClient tokenClient = createClient()) {
            Member payer = createMemberAndLinkAccounts(tokenClient);
            Member payee = tokenClient.createMemberBlocking(randomAlias());

            Token token = createTransferTokenWithOtherOptions(payer, payee.memberId());
            assertThat(token).isNotNull();
        }
    }

    @Test
    public void createPaymentTokenToDestinationTest() {
        try (TokenClient tokenClient = createClient()) {
            Member payer = createMemberAndLinkAccounts(tokenClient);
            Alias payeeAlias = randomAlias();
            Member payee = tokenClient.createMemberBlocking(payeeAlias);
            Token token = createTransferTokenToDestination(payer, payeeAlias);
            assertThat(token).isNotNull();
        }
    }

    @Test
    public void createPaymentTokenScheduledTest() {
        try (TokenClient tokenClient = createClient()) {
            Member payer = createMemberAndLinkAccounts(tokenClient);
            Alias payeeAlias = randomAlias();
            Member payee = tokenClient.createMemberBlocking(payeeAlias);
            Token token = createTransferTokenScheduled(payer, payeeAlias);
            assertThat(token).isNotNull();
        }
    }
}
