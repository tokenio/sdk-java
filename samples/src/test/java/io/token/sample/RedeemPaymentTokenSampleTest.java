package io.token.sample;

import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.proto.common.transfer.TransferProtos;

import org.junit.Test;

public class RedeemPaymentTokenSampleTest {
    @Test
    public void redeemPaymentTokenSampleTest() {
        String tokenApiUrl = "api-grpc.dev.token.io";
        String bankApiUrl = "fank-grpc.dev.token.io";
        Member payer = LinkMemberAndBankSample.linkBank(tokenApiUrl, bankApiUrl);
        Member payee = LinkMemberAndBankSample.linkBank(tokenApiUrl, bankApiUrl);

        TransferProtos.Transfer transfer = RedeemPaymentTokenSample.redeemToken(payer, payee);
        assertThat(transfer).isNotNull();
    }
}
