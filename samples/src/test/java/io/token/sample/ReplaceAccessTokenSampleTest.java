package io.token.sample;

import static io.token.TokenIO.TokenCluster.DEVELOPMENT;
import static io.token.sample.CreateAndEndorseAccessTokenSample.createAccessToken;
import static io.token.sample.ReplaceAccessTokenSample.findAccessToken;
import static io.token.sample.ReplaceAccessTokenSample.replaceAccessToken;
import static io.token.sample.TestUtil.newAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.token.TokenProtos.Token;

import java.util.Optional;

import org.junit.Test;

public class ReplaceAccessTokenSampleTest {
    @Test
    public void getAccessTokensTest() {
        try (TokenIO tokenIO = TokenIO.create(
                DEVELOPMENT,
                "4qY7lqQw8NOl9gng0ZHgT4xdiDqxqoGVutuZwrUYQsI")) {
            Member grantor = tokenIO.createMember(newAlias());
            Member grantee = tokenIO.createMember(newAlias());

            Token createdToken = createAccessToken(grantor, grantee.firstAlias());
            Optional<Token> foundToken = findAccessToken(
                    grantor,
                    grantee.firstAlias());
            assertThat(foundToken.get()).isEqualTo(createdToken);
        }
    }

    @Test
    public void replaceAccessTokenTest() {
        try (TokenIO tokenIO = TokenIO.create(
                DEVELOPMENT,
                "4qY7lqQw8NOl9gng0ZHgT4xdiDqxqoGVutuZwrUYQsI")) {
            Member grantor = tokenIO.createMember(newAlias());
            Member grantee = tokenIO.createMember(newAlias());

            Token createdToken = createAccessToken(grantor, grantee.firstAlias());
            replaceAccessToken(grantor, grantee.firstAlias());

            Optional<Token> foundToken = findAccessToken(
                    grantor,
                    grantee.firstAlias());

            assertThat(foundToken.get().getPayload().getAccess().getResourcesCount()).isEqualTo(2);
        }
    }

    @Test
    public void replaceAndEndorseAccessTokenTest() {
        try (TokenIO tokenIO = TokenIO.create(
                DEVELOPMENT,
                "4qY7lqQw8NOl9gng0ZHgT4xdiDqxqoGVutuZwrUYQsI")) {
            Member grantor = tokenIO.createMember(newAlias());
            Member grantee = tokenIO.createMember(newAlias());

            Token createdToken = createAccessToken(grantor, grantee.firstAlias());
            ReplaceAccessTokenSample.replaceAndEndorseAccessToken(grantor, grantee.firstAlias());

            Optional<Token> foundToken = findAccessToken(
                    grantor,
                    grantee.firstAlias());

            assertThat(foundToken.get().getPayload().getAccess().getResourcesCount()).isEqualTo(2);
        }
    }
}
