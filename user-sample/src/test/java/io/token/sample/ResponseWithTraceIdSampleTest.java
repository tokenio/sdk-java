package io.token.sample;

import static io.token.sample.CreateAndEndorseAccessTokenSample.createBalanceAccessToken;
import static io.token.sample.ResponseWithTraceIdSample.cancelAccessTokenAndReturnTraceId;
import static io.token.sample.TestUtil.createClient;

import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.WithTraceId;
import io.token.proto.common.alias.AliasProtos;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.user.Member;
import io.token.user.TokenClient;

import org.junit.Test;

public class ResponseWithTraceIdSampleTest {
    @Test
    public void responseWithTraceIdTest() {
        try (TokenClient tokenClient = createClient()) {
            Member grantor = tokenClient.createMemberBlocking(randomAlias());
            String accountId = grantor.createTestBankAccountBlocking(1000.0, "EUR")
                    .id();
            AliasProtos.Alias granteeAlias = randomAlias();
            Member grantee = tokenClient.createMemberBlocking(granteeAlias);

            Token token = createBalanceAccessToken(grantor, accountId, granteeAlias);
            WithTraceId<TokenOperationResult> result = cancelAccessTokenAndReturnTraceId(
                    grantor,
                    token.getId());

            assertThat(result.getTraceId()).isNotBlank();
        }
    }
}
