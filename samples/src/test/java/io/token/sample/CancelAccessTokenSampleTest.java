package io.token.sample;

import static io.token.Token.TokenCluster.DEVELOPMENT;
import static io.token.sample.CancelAccessTokenSample.cancelAccessToken;
import static io.token.sample.CreateAndEndorseAccessTokenSample.createAccessToken;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;

import org.junit.Test;

public class CancelAccessTokenSampleTest {
    @Test
    public void cancelAccessTokenByGrantorTest() {
        Member grantor = CreateMemberSample.createMember(DEVELOPMENT);
        Member grantee = CreateMemberSample.createMember(DEVELOPMENT);

        Token token = createAccessToken(grantor, grantee.firstUsername());
        TokenOperationResult result = cancelAccessToken(grantor, token.getId());
        assertThat(result.getStatus()).isEqualTo(TokenOperationResult.Status.SUCCESS);
    }
}
