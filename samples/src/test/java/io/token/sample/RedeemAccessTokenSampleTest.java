package io.token.sample;

import static io.token.Token.TokenCluster.DEVELOPMENT;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Account;
import io.token.Member;
import io.token.proto.common.token.TokenProtos.Token;

import java.util.List;
import org.junit.Test;

public class RedeemAccessTokenSampleTest {
    @Test
    public void redeemAccessTokenTest() {
        Member grantor = LinkMemberAndBankSample.linkBank(DEVELOPMENT);
        Member grantee = LinkMemberAndBankSample.linkBank(DEVELOPMENT);

        Token token =
                CreateAndEndorseAccessTokenSample.createToken(grantor, grantee.firstUsername());

        List<Account> grantorAccounts = RedeemAccessTokenSample.redeemToken(grantee, token.getId());
        assertThat(grantorAccounts.isEmpty()).isFalse();
    }
}
