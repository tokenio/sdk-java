package io.token.sample;

import static io.token.Token.TokenCluster.DEVELOPMENT;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transfer.TransferProtos.Transfer;

import org.junit.Test;

public class RedeemPaymentTokenSampleTest {
    @Test
    public void redeemPaymentTokenTest() {
        Member payer = LinkMemberAndBankSample.linkBank(DEVELOPMENT);
        Member payee = LinkMemberAndBankSample.linkBank(DEVELOPMENT);

        Token token = CreateAndEndorsePaymentTokenSample.createToken(payer, payee.firstUsername());

        Transfer transfer = RedeemPaymentTokenSample.redeemToken(payee, token.getId());
        assertThat(transfer).isNotNull();
    }
}
