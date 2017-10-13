package io.token.sample;

import static io.token.sample.CreateAndEndorseTransferTokenSample.createTransferToken;
import static io.token.sample.CreateAndEndorseTransferTokenSample.createTransferTokenToDestination;
import static io.token.sample.CreateAndEndorseTransferTokenSample.createTransferTokenWithOtherOptions;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createMemberAndLinkAccounts;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos.Token;

import org.junit.Test;

public class CreateAndEndorseTransferTokenSampleTest {
    @Test
    public void createPaymentTokenTest() {
        try (TokenIO tokenIO = createClient()) {
            Member payer = createMemberAndLinkAccounts(tokenIO);
            Alias payeeAlias = randomAlias();
            Member payee = tokenIO.createMember(payeeAlias);

            Token token = createTransferToken(payer, payeeAlias);
            assertThat(token).isNotNull();
        }
    }

    @Test
    public void createPaymentTokenWithOtherOptionsTest() {
        try (TokenIO tokenIO = createClient()) {
            Member payer = createMemberAndLinkAccounts(tokenIO);
            Member payee = tokenIO.createMember(randomAlias());

            Token token = createTransferTokenWithOtherOptions(payer, payee.memberId());
            assertThat(token).isNotNull();
        }
    }

    @Test
    public void createPaymentTokenToDestinationTest() {
        try (TokenIO tokenIO = createClient()) {
            Member payer = createMemberAndLinkAccounts(tokenIO);
            Alias payeeAlias = randomAlias();
            Member payee = tokenIO.createMember(payeeAlias);

            Token token = createTransferTokenToDestination(payer, payeeAlias);
            assertThat(token).isNotNull();
        }
    }
}
