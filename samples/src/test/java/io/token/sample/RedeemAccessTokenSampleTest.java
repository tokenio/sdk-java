package io.token.sample;

import static io.token.Token.TokenCluster.DEVELOPMENT;
import static io.token.sample.CreateAndEndorseAccessTokenSample.createAccessToken;
import static io.token.sample.RedeemAccessTokenSample.redeemAccessToken;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Account;
import io.token.Member;
import io.token.proto.common.token.TokenProtos.Token;

import java.util.List;
import org.junit.Test;

public class RedeemAccessTokenSampleTest {
    @Test
    public void redeemAccessTokenTest() {
        Member grantor = LinkMemberAndBankSample.linkBankAccounts(DEVELOPMENT);
        Member grantee = LinkMemberAndBankSample.linkBankAccounts(DEVELOPMENT);

        Token token = createAccessToken(grantor, grantee.firstUsername());

        List<Account> grantorAccounts = redeemAccessToken(grantee, token.getId());
        assertThat(grantorAccounts.isEmpty()).isFalse();
    }
}
