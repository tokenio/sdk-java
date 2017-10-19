package io.token.sample;

import static io.token.sample.CancelTransferTokenSample.cancelTransferToken;
import static io.token.sample.CreateAndEndorseTransferTokenSample.createTransferToken;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createMemberAndLinkAccounts;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;

import org.junit.Test;

public class CancelTransferTokenSampleTest {
    @Test
    public void cancelTransferTokenByGrantorTest() {
        try (TokenIO tokenIO = createClient()) {
            Member grantor = createMemberAndLinkAccounts(tokenIO);
            Alias granteeAlias = randomAlias();
            tokenIO.createMember(granteeAlias);

            Token token = createTransferToken(grantor, granteeAlias);
            TokenOperationResult result = cancelTransferToken(grantor, token.getId());
            assertThat(result.getStatus()).isEqualTo(TokenOperationResult.Status.SUCCESS);
        }
    }
}
