package io.token.sample;

import static io.token.Token.TokenCluster.DEVELOPMENT;
import static io.token.sample.CreateAndEndorseAccessTokenSample.createAccessToken;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.proto.common.token.TokenProtos.Token;

import org.junit.Test;

public class CreateAndEndorseAccessTokenSampleTest {
    @Test
    public void createAccessTokenTest() {
        Member grantor = LinkMemberAndBankSample.linkBankAccounts(DEVELOPMENT);
        Member grantee = LinkMemberAndBankSample.linkBankAccounts(DEVELOPMENT);

        Token token = createAccessToken(grantor, grantee.firstUsername());
        assertThat(token).isNotNull();
    }
}
