package io.token.sample;

import static io.token.sample.CreateAndEndorseTransferTokenSample.createTransferToken;
import static io.token.sample.CreateAndEndorseTransferTokenSample.createTransferTokenToDestination;
import static io.token.sample.CreateAndEndorseTransferTokenSample.createTransferTokenWithOtherOptions;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createMemberAndLinkAccounts;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.user.Member;
import io.token.user.TokenClient;

import org.junit.Test;

public class CreateAndEndorseTransferTokenSampleTest {
    @Test
    public void createPaymentTokenTest() {
        try (TokenClient tokenClient = createClient()) {
            Member payer = createMemberAndLinkAccounts(tokenClient);
            Alias payeeAlias = randomAlias();
            Member payee = tokenClient.createMemberBlocking(payeeAlias);
            Token token = createTransferToken(payer, payeeAlias);
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
}
