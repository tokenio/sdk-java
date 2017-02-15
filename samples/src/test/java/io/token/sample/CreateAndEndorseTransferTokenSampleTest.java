package io.token.sample;

import static io.token.TokenIO.TokenCluster.DEVELOPMENT;
import static io.token.sample.CreateAndEndorseTransferTokenSample.createTransferToken;
import static io.token.sample.TokenFactory.newUserName;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.token.TokenProtos.Token;

import org.junit.Test;

public class CreateAndEndorseTransferTokenSampleTest {
    @Test
    public void createPaymentTokenTest() {
        try (TokenIO tokenIO = TokenFactory.newSdk(DEVELOPMENT)) {
            Member payer = tokenIO.createMember(newUserName());
            Member payee = tokenIO.createMember(newUserName());

            LinkMemberAndBankSample.linkBankAccounts(payer);

            Token token = createTransferToken(payer, payee.firstUsername());
            assertThat(token).isNotNull();
        }
    }
}
