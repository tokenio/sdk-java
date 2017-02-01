package io.token.sample;

import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transfer.TransferProtos.Transfer;

import org.junit.Test;

public class RedeemPaymentTokenSampleTest {
    @Test
    public void redeemPaymentTokenTest() {
        String tokenApiUrl = "api-grpc.dev.token.io";
        String bankApiUrl = "fank-grpc.dev.token.io";
        Member payer = LinkMemberAndBankSample.linkBank(tokenApiUrl, bankApiUrl);
        Member payee = LinkMemberAndBankSample.linkBank(tokenApiUrl, bankApiUrl);

        Token token = CreateAndEndorsePaymentTokenSample.createToken(payer, payee.firstUsername());

        Transfer transfer = RedeemPaymentTokenSample.redeemToken(payee, token.getId());
        assertThat(transfer).isNotNull();
    }
}
