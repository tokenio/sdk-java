package io.token.sample;

import static org.assertj.core.api.Assertions.assertThat;

import io.token.Account;
import io.token.Member;
import io.token.proto.common.token.TokenProtos.Token;

import java.util.List;
import org.junit.Test;

public class RedeemAccessTokenSampleTest {
    @Test
    public void redeemAccessTokenTest() {
        String tokenApiUrl = "api-grpc.dev.token.io";
        String bankApiUrl = "fank-grpc.dev.token.io";
        Member grantor = LinkMemberAndBankSample.linkBank(tokenApiUrl, bankApiUrl);
        Member grantee = LinkMemberAndBankSample.linkBank(tokenApiUrl, bankApiUrl);

        Token token =
                CreateAndEndorseAccessTokenSample.createToken(grantor, grantee.firstUsername());

        List<Account> grantorAccounts = RedeemAccessTokenSample.redeemToken(grantee, token.getId());
        assertThat(grantorAccounts.isEmpty()).isFalse();
    }
}
