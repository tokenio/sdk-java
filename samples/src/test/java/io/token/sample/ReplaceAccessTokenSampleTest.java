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

import org.junit.Test;

public class ReplaceAccessTokenSampleTest {
    @Test
    public void getAccessTokensTest() {
        try (TokenIO tokenIO = createClient()) {
            Member grantor = tokenIO.createMember(randomAlias());
            String accountId = grantor.createAndLinkTestBankAccount(1000, "EUR").id();
            Alias granteeAlias = randomAlias();
            Member grantee = tokenIO.createMember(granteeAlias);
            Token createdToken = createAccessToken(grantor, accountId, granteeAlias);
            Token foundToken = findAccessToken(tokenIO, grantor, granteeAlias);
            assertThat(foundToken.getId()).isEqualTo(createdToken.getId());
        }
    }

    @Test
    public void replaceAccessTokenTest() {
        try (TokenIO tokenIO = createClient()) {
            Member grantor = tokenIO.createMember(randomAlias());
            String accountId = grantor.createAndLinkTestBankAccount(1000, "EUR").id();
            Alias granteeAlias = randomAlias();
            Member grantee = tokenIO.createMember(granteeAlias);
            createAccessToken(grantor, accountId, granteeAlias);
            Token activeToken = findAccessToken(tokenIO, grantor, granteeAlias);

            replaceAccessToken(grantor, granteeAlias, activeToken);

            assertThat(findAccessToken(tokenIO, grantor, granteeAlias))
                    .isNotEqualTo(activeToken.getId());
        }
    }

    @Test
    public void replaceAndEndorseAccessTokenTest() {
        try (TokenIO tokenIO = createClient()) {
            Member grantor = tokenIO.createMember(randomAlias());
            String accountId = grantor.createAndLinkTestBankAccount(1000, "EUR").id();
            Alias granteeAlias = randomAlias();
            Member grantee = tokenIO.createMember(granteeAlias);
            createAccessToken(grantor, accountId, granteeAlias);
            Token activeToken = findAccessToken(tokenIO, grantor, granteeAlias);

            ReplaceAccessTokenSample.replaceAndEndorseAccessToken(
                    grantor,
                    granteeAlias,
                    activeToken);

            activeToken = findAccessToken(tokenIO, grantor, granteeAlias);
            assertThat(activeToken.getPayload().getAccess().getResourcesCount()).isEqualTo(1);
        }
    }
}
