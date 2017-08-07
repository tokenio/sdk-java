package io.token.sample;

import static io.token.TokenIO.TokenCluster.DEVELOPMENT;
import static io.token.sample.CreateAndEndorseTransferTokenSample.createTransferToken;
import static io.token.sample.CreateAndEndorseTransferTokenSample.createTransferTokenFromAuth;
import static io.token.sample.CreateAndEndorseTransferTokenSample.createTransferTokenToDestination;
import static io.token.sample.CreateAndEndorseTransferTokenSample.createTransferTokenWithOtherOptions;
import static io.token.sample.TestUtil.newUserName;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.token.TokenProtos.Token;

import org.junit.Test;

public class CreateAndEndorseTransferTokenSampleTest {
    @Test
    public void createPaymentTokenTest() {
        try (TokenIO tokenIO = TokenIO.create(DEVELOPMENT)) {
            Member payer = tokenIO.createMember(newUserName());
            Member payee = tokenIO.createMember(newUserName());

            LinkMemberAndBankSample.linkBankAccounts(payer);

            Token token = createTransferToken(payer, payee.firstUsername());
            assertThat(token).isNotNull();
        }
    }

    @Test
    public void createPaymentTokenWithOtherOptionsTest() {
        try (TokenIO tokenIO = TokenIO.create(DEVELOPMENT)) {
            Member payer = tokenIO.createMember(newUserName());
            Member payee = tokenIO.createMember(newUserName());

            LinkMemberAndBankSample.linkBankAccounts(payer);

            Token token = createTransferTokenWithOtherOptions(payer, payee.memberId());
            assertThat(token).isNotNull();
        }
    }

    @Test
    public void createPaymentTokenFromAuthTest() {
        try (TokenIO tokenIO = TokenIO.create(DEVELOPMENT)) {
            Member payer = tokenIO.createMember(newUserName());
            Member payee = tokenIO.createMember(newUserName());

            Token token = createTransferTokenFromAuth(payer, payee.firstUsername());
            assertThat(token).isNotNull();
        }
    }

    @Test
    public void createPaymentTokenToDestinationTest() {
        try (TokenIO tokenIO = TokenIO.create(DEVELOPMENT)) {
            Member payer = tokenIO.createMember(newUserName());
            Member payee = tokenIO.createMember(newUserName());

            LinkMemberAndBankSample.linkBankAccounts(payer);

            Token token = createTransferTokenToDestination(payer, payee.firstUsername());
            assertThat(token).isNotNull();
        }
    }
}
