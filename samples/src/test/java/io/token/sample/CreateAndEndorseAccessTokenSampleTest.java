package io.token.sample;

import static io.token.sample.CreateAndEndorseAccessTokenSample.createAccessToken;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos.Token;

import org.junit.Test;

public class CreateAndEndorseAccessTokenSampleTest {
    @Test
    public void createAccessTokenTest() {
        try (TokenIO tokenIO = createClient()) {
            Member grantor = tokenIO.createMember(randomAlias());
            Alias granteeAlias = randomAlias();
            Member grantee = tokenIO.createMember(granteeAlias);

            Token token = createAccessToken(grantor, granteeAlias);
            assertThat(token).isNotNull();
        }
    }
}
