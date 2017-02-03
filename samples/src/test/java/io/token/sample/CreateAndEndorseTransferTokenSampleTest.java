package io.token.sample;

import static io.token.Token.TokenCluster.DEVELOPMENT;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.proto.common.token.TokenProtos.Token;

import org.junit.Test;

public class CreateAndEndorseTransferTokenSampleTest {
    @Test
    public void createPaymentTokenTest() {
        Member payer = LinkMemberAndBankSample.linkBank(DEVELOPMENT);
        Member payee = LinkMemberAndBankSample.linkBank(DEVELOPMENT);

        Token token = CreateAndEndorseTransferTokenSample.createToken(payer, payee.firstUsername());
        assertThat(token).isNotNull();
    }
}