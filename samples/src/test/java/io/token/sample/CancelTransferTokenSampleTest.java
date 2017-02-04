package io.token.sample;

import static io.token.Token.TokenCluster.DEVELOPMENT;
import static io.token.sample.CancelTransferTokenSample.cancelTransferToken;
import static io.token.sample.CreateAndEndorseAccessTokenSample.createAccessToken;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;

import org.junit.Test;

public class CancelTransferTokenSampleTest {
    @Test
    public void cancelTransferTokenByGrantorTest() {
        Member grantor = LinkMemberAndBankSample.linkBank(DEVELOPMENT);
        Member grantee = LinkMemberAndBankSample.linkBank(DEVELOPMENT);

        Token token = createAccessToken(grantor, grantee.firstUsername());
        TokenOperationResult result = cancelTransferToken(grantor, token.getId());
        assertThat(result.getStatus()).isEqualTo(TokenOperationResult.Status.SUCCESS);
    }
}
