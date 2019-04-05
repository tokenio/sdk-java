package io.token.sample;

import static io.token.sample.CancelTransferTokenSample.cancelTransferToken;
import static io.token.sample.CreateAndEndorseTransferTokenSample.createTransferToken;
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

public class CancelTransferTokenSampleTest {
    @Test
    public void cancelTransferTokenByGrantorTest() {
        try (TokenClient tokenClient = createClient()) {
            Member payer = createMemberAndLinkAccounts(tokenClient);
            Alias granteeAlias = randomAlias();
            Member payee = tokenClient.createMemberBlocking(granteeAlias);

            Token token = createTransferToken(payer, granteeAlias);
            TokenOperationResult result = cancelTransferToken(payer, token.getId());
            assertThat(result.getStatus()).isEqualTo(TokenOperationResult.Status.SUCCESS);
        }
    }
}
