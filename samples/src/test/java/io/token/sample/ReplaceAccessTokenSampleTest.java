package io.token.sample;

import static io.token.TokenIO.TokenCluster.DEVELOPMENT;
import static io.token.sample.CreateAndEndorseAccessTokenSample.createAccessToken;
import static io.token.sample.ReplaceAccessTokenSample.findAccessToken;
import static io.token.sample.ReplaceAccessTokenSample.replaceAccessToken;
import static io.token.sample.TestUtil.newUserName;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.token.TokenProtos.Token;

import java.util.Optional;

import org.junit.Test;

public class ReplaceAccessTokenSampleTest {
    @Test
    public void getAccessTokensTest() {
        try (TokenIO tokenIO = TokenIO.create(DEVELOPMENT)) {
            Member grantor = tokenIO.createMember(newUserName());
            Member grantee = tokenIO.createMember(newUserName());

            Token createdToken = createAccessToken(grantor, grantee.firstUsername());
            Optional<Token> foundToken = findAccessToken(
                    grantor,
                    grantee.firstUsername());
            assertThat(foundToken.get()).isEqualTo(createdToken);
        }
    }

    @Test
    public void replaceAccessTokenTest() {
        try (TokenIO tokenIO = TokenIO.create(DEVELOPMENT)) {
            Member grantor = tokenIO.createMember(newUserName());
            Member grantee = tokenIO.createMember(newUserName());

            Token createdToken = createAccessToken(grantor, grantee.firstUsername());
            replaceAccessToken(grantor, grantee.firstUsername());

            Optional<Token> foundToken = findAccessToken(
                    grantor,
                    grantee.firstUsername());

            assertThat(foundToken.get().getPayload().getAccess().getResourcesCount()).isEqualTo(2);
        }
    }

    @Test
    public void replaceAndEndorseAccessTokenTest() {
        try (TokenIO tokenIO = TokenIO.create(DEVELOPMENT)) {
            Member grantor = tokenIO.createMember(newUserName());
            Member grantee = tokenIO.createMember(newUserName());

            Token createdToken = createAccessToken(grantor, grantee.firstUsername());
            ReplaceAccessTokenSample.replaceAndEndorseAccessToken(grantor, grantee.firstUsername());

            Optional<Token> foundToken = findAccessToken(
                    grantor,
                    grantee.firstUsername());

            assertThat(foundToken.get().getPayload().getAccess().getResourcesCount()).isEqualTo(2);
        }
    }
}
