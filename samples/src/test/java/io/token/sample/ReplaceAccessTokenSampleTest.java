package io.token.sample;

import static io.token.sample.CreateAndEndorseAccessTokenSample.createAccessToken;
import static io.token.sample.ReplaceAccessTokenSample.findAccessToken;
import static io.token.sample.ReplaceAccessTokenSample.replaceAccessToken;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos.Token;

import java.util.Optional;

import org.junit.Test;

public class ReplaceAccessTokenSampleTest {
    @Test
    public void getAccessTokensTest() {
        try (TokenIO tokenIO = createClient()) {
            Member grantor = tokenIO.createMember(randomAlias());
            Alias granteeAlias = randomAlias();
            Member grantee = tokenIO.createMember(granteeAlias);

            Token createdToken = createAccessToken(grantor, granteeAlias);
            Optional<Token> foundToken = findAccessToken(grantor, granteeAlias);
            assertThat(foundToken.get()).isEqualTo(createdToken);
        }
    }

    @Test
    public void replaceAccessTokenTest() {
        try (TokenIO tokenIO = createClient()) {
            Member grantor = tokenIO.createMember(randomAlias());
            Alias granteeAlias = randomAlias();
            Member grantee = tokenIO.createMember(granteeAlias);

            Token createdToken = createAccessToken(grantor, granteeAlias);
            replaceAccessToken(grantor, granteeAlias);

            Optional<Token> foundToken = findAccessToken(grantor, granteeAlias);

            assertThat(foundToken.get().getPayload().getAccess().getResourcesCount()).isEqualTo(2);
        }
    }

    @Test
    public void replaceAndEndorseAccessTokenTest() {
        try (TokenIO tokenIO = createClient()) {
            Member grantor = tokenIO.createMember(randomAlias());
            Alias granteeAlias = randomAlias();
            Member grantee = tokenIO.createMember(granteeAlias);

            Token createdToken = createAccessToken(grantor, granteeAlias);
            ReplaceAccessTokenSample.replaceAndEndorseAccessToken(grantor, granteeAlias);

            Optional<Token> foundToken = findAccessToken(grantor, granteeAlias);

            assertThat(foundToken.get().getPayload().getAccess().getResourcesCount()).isEqualTo(2);
        }
    }
}
