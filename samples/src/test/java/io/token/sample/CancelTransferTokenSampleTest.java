package io.token.sample;

import static io.token.TokenIO.TokenCluster.DEVELOPMENT;
import static io.token.sample.CancelTransferTokenSample.cancelTransferToken;
import static io.token.sample.CreateAndEndorseTransferTokenSample.createTransferToken;
import static io.token.sample.TestUtil.newAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;

import org.junit.Test;

public class CancelTransferTokenSampleTest {
    @Test
    public void cancelTransferTokenByGrantorTest() {
        try (TokenIO tokenIO = TokenIO.create(DEVELOPMENT, "devKey")) {
            Member grantor = tokenIO.createMember(newAlias());
            Member grantee = tokenIO.createMember(newAlias());

            LinkMemberAndBankSample.linkBankAccounts(grantor);

            Token token = createTransferToken(grantor, grantee.firstAlias());
            TokenOperationResult result = cancelTransferToken(grantor, token.getId());
            assertThat(result.getStatus()).isEqualTo(TokenOperationResult.Status.SUCCESS);
        }
    }
}
