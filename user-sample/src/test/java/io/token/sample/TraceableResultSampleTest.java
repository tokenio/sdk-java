package io.token.sample;

import static io.token.sample.CreateAndEndorseAccessTokenSample.createBalanceAccessToken;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.randomAlias;
import static io.token.sample.TraceableResultSample.cancelAccessTokenAndReturnTraceId;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.TraceableResult;
import io.token.proto.common.alias.AliasProtos;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.user.Member;
import io.token.user.TokenClient;

import org.junit.Test;

public class TraceableResultSampleTest {
    @Test
    public void responseWithTraceIdTest() {
        try (TokenClient tokenClient = createClient()) {
            Member grantor = tokenClient.createMemberBlocking(randomAlias());
            String accountId = grantor.createTestBankAccountBlocking(1000.0, "EUR")
                    .id();
            AliasProtos.Alias granteeAlias = randomAlias();
            Member grantee = tokenClient.createMemberBlocking(granteeAlias);

            Token token = createBalanceAccessToken(grantor, accountId, granteeAlias);
            TraceableResult<TokenOperationResult> result = cancelAccessTokenAndReturnTraceId(
                    grantor,
                    token.getId());

            assertThat(result.getTraceId()).isNotBlank();
        }
    }
}
