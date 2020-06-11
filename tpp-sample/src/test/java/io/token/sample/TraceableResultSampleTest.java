package io.token.sample;

import static io.token.sample.CreateAndEndorseAccessTokenSample.createBalanceAccessToken;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createUserMember;
import static io.token.sample.TestUtil.randomAlias;
import static io.token.sample.TraceableResultSample.cancelAccessTokenAndReturnTraceId;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.TraceableResult;
import io.token.proto.common.alias.AliasProtos;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;

import org.junit.Test;

public class TraceableResultSampleTest {
    @Test
    public void responseWithTraceIdTest() {
        try (TokenClient tokenClient = createClient()) {
            io.token.user.Member grantor = createUserMember();
            String accountId = grantor.getAccountsBlocking().get(0).id();
            AliasProtos.Alias granteeAlias = randomAlias();
            Member grantee = tokenClient.createMemberBlocking(granteeAlias);

            Token token = createBalanceAccessToken(grantor, accountId, granteeAlias);
            TraceableResult<TokenOperationResult> result = cancelAccessTokenAndReturnTraceId(
                    grantee,
                    token.getId());
            assertThat(result.getTraceId()).isNotBlank();
        }
    }
}
