package io.token.sample;

import static io.token.sample.CancelAccessTokenSample.cancelAccessToken;
import static io.token.sample.CreateAndEndorseAccessTokenSample.createAccessToken;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.user.Member;
import io.token.user.TokenClient;

import org.junit.Test;

public class CancelAccessTokenSampleTest {
    @Test
    public void cancelAccessTokenByGrantorTest() {
        try (TokenClient tokenClient = createClient()) {
            Member grantor = tokenClient.createMemberBlocking(randomAlias());
            String accountId = grantor.createAndLinkTestBankAccountBlocking(1000.0, "EUR")
                    .id();
            Alias granteeAlias = randomAlias();
            Member grantee = tokenClient.createMemberBlocking(granteeAlias);

            Token token = createAccessToken(grantor, accountId, granteeAlias);
            TokenOperationResult result = cancelAccessToken(grantor, token.getId());
            assertThat(result.getStatus()).isEqualTo(TokenOperationResult.Status.SUCCESS);
        }
    }
}
