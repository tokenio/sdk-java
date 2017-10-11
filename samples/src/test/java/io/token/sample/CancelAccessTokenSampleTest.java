package io.token.sample;

import static io.token.TokenIO.TokenCluster.DEVELOPMENT;
import static io.token.sample.CancelAccessTokenSample.cancelAccessToken;
import static io.token.sample.CreateAndEndorseAccessTokenSample.createAccessToken;
import static io.token.sample.TestUtil.newAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;

import org.junit.Test;

public class CancelAccessTokenSampleTest {
    @Test
    public void cancelAccessTokenByGrantorTest() {
        try (TokenIO tokenIO = TokenIO.create(DEVELOPMENT)) {
            Member grantor = tokenIO.createMember(newAlias());
            Alias granteeAlias = newAlias();
            Member grantee = tokenIO.createMember(granteeAlias);

            Token token = createAccessToken(grantor, granteeAlias);
            TokenOperationResult result = cancelAccessToken(grantor, token.getId());
            assertThat(result.getStatus()).isEqualTo(TokenOperationResult.Status.SUCCESS);
        }
    }
}
