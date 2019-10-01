package io.token.sample;

import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.sample.CancelTokenSample.cancelAccessToken;
import static io.token.sample.CancelTokenSample.cancelTransferToken;
import static io.token.sample.CreateAndEndorseAccessTokenSample.createBalanceAccessToken;
import static io.token.sample.CreateTransferTokenSample.createTransferToken;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createMemberAndLinkAccounts;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.user.Member;
import io.token.user.TokenClient;

import org.junit.Test;

public class CancelTokenSampleTest {
    @Test
    public void cancelAccessTokenByGrantorTest() {
        try (TokenClient tokenClient = createClient()) {
            Member grantor = tokenClient.createMemberBlocking(randomAlias());
            String accountId = grantor.createTestBankAccountBlocking(1000.0, "EUR")
                    .id();
            Alias granteeAlias = randomAlias();
            Member grantee = tokenClient.createMemberBlocking(granteeAlias);

            Token token = createBalanceAccessToken(grantor, accountId, granteeAlias);
            TokenOperationResult result = cancelAccessToken(grantor, token.getId());
            assertThat(result.getStatus()).isEqualTo(TokenOperationResult.Status.SUCCESS);
        }
    }

    @Test
    public void cancelTransferTokenByGrantorTest() {
        try (TokenClient tokenClient = createClient()) {
            Member payer = createMemberAndLinkAccounts(tokenClient);
            Alias granteeAlias = randomAlias();
            Member payee = tokenClient.createMemberBlocking(granteeAlias);

            Token token = createTransferToken(payer, granteeAlias, LOW);
            TokenOperationResult result = cancelTransferToken(payer, token.getId());
            assertThat(result.getStatus()).isEqualTo(TokenOperationResult.Status.SUCCESS);
        }
    }
}
