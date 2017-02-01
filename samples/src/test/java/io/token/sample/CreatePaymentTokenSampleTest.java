package io.token.sample;

import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.proto.common.token.TokenProtos.Token;

import org.junit.Test;

public class CreatePaymentTokenSampleTest {
    @Test
    public void createPaymentTokenTest() {
        String tokenApiUrl = "api-grpc.dev.token.io";
        String bankApiUrl = "fank-grpc.dev.token.io";
        Member payer = LinkMemberAndBankSample.linkBank(tokenApiUrl, bankApiUrl);
        Member payee = LinkMemberAndBankSample.linkBank(tokenApiUrl, bankApiUrl);

        Token token = CreatePaymentTokenSample.createToken(payer, payee.firstUsername());
        assertThat(token).isNotNull();
    }
}
