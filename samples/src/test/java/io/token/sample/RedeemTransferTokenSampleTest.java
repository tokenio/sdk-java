package io.token.sample;

import static io.token.Token.TokenCluster.DEVELOPMENT;
import static io.token.sample.CreateAndEndorseTransferTokenSample.createTransferToken;
import static io.token.sample.RedeemTransferTokenSample.redeemTransferToken;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transfer.TransferProtos.Transfer;

import org.junit.Test;

public class RedeemTransferTokenSampleTest {
    @Test
    public void redeemPaymentTokenTest() {
        Member payer = LinkMemberAndBankSample.linkBank(DEVELOPMENT);
        Member payee = LinkMemberAndBankSample.linkBank(DEVELOPMENT);

        Token token = createTransferToken(payer, payee.firstUsername());

        Transfer transfer = redeemTransferToken(payee, token.getId());
        assertThat(transfer).isNotNull();
    }
}
