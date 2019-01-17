package io.token.sample;

import static io.token.sample.CreateAndEndorseAccessTokenSample.createAccessToken;
import static io.token.sample.ReplaceAccessTokenSample.findAccessToken;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;

import org.junit.Test;

public class ReplaceAccessTokenSampleTest {
    @Test
    public void getAccessTokensTest() {
        try (TokenClient tokenClient = createClient()) {
            Member grantor = tokenClient.createMemberBlocking(randomAlias());
            String accountId = grantor.createAndLinkTestBankAccountBlocking(1000, "EUR").id();
            Alias granteeAlias = randomAlias();
            Member grantee = tokenClient.createMemberBlocking(granteeAlias);
            Token createdToken = createAccessToken(grantor, accountId, granteeAlias);
            Token foundToken = findAccessToken(tokenClient, grantor, granteeAlias);
            assertThat(foundToken.getId()).isEqualTo(createdToken.getId());
        }
    }
}