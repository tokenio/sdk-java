package io.token.sample;

import static io.token.TokenIO.TokenCluster.DEVELOPMENT;
import static io.token.proto.common.testing.Sample.alias;
import static io.token.sample.CancelAccessTokenSample.cancelAccessToken;
import static io.token.sample.CreateAndEndorseAccessTokenSample.createAccessToken;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;

import org.junit.Test;

public class CancelAccessTokenSampleTest {
    @Test
    public void cancelAccessTokenByGrantorTest() {
        try (TokenIO tokenIO = TokenIO.create(DEVELOPMENT)) {
            Member grantor = tokenIO.createMember(alias());
            Member grantee = tokenIO.createMember(alias());

            Token token = createAccessToken(grantor, grantee.firstAlias());
            TokenOperationResult result = cancelAccessToken(grantor, token.getId());
            assertThat(result.getStatus()).isEqualTo(TokenOperationResult.Status.SUCCESS);
        }
    }
}
