package io.token.sample;

import static io.token.Token.TokenCluster.DEVELOPMENT;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.proto.common.token.TokenProtos.Token;

import org.junit.Test;

public class CreateAndEndorseAccessTokenSampleTest {
    @Test
    public void createAccessTokenTest() {
        Member grantor = LinkMemberAndBankSample.linkBank(DEVELOPMENT);
        Member grantee = LinkMemberAndBankSample.linkBank(DEVELOPMENT);

        Token token =
                CreateAndEndorseAccessTokenSample.createToken(grantor, grantee.firstUsername());
        assertThat(token).isNotNull();
    }
}
