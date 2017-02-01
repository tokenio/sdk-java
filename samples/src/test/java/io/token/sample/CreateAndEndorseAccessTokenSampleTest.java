package io.token.sample;

import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.proto.common.token.TokenProtos.Token;

import org.junit.Test;

public class CreateAndEndorseAccessTokenSampleTest {
    @Test
    public void createAccessTokenTest() {
        String tokenApiUrl = "api-grpc.dev.token.io";
        String bankApiUrl = "fank-grpc.dev.token.io";
        Member grantor = LinkMemberAndBankSample.linkBank(tokenApiUrl, bankApiUrl);
        Member grantee = LinkMemberAndBankSample.linkBank(tokenApiUrl, bankApiUrl);

        Token token =
                CreateAndEndorseAccessTokenSample.createToken(grantor, grantee.firstUsername());
        assertThat(token).isNotNull();
    }
}
