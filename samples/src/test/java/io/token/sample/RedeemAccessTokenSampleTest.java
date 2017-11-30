package io.token.sample;

import static io.token.sample.CreateAndEndorseAccessTokenSample.createAccessToken;
import static io.token.sample.RedeemAccessTokenSample.redeemAccessToken;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createMemberAndLinkAccounts;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.MoneyUtil;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.token.TokenProtos.Token;

import java.math.BigDecimal;

import org.junit.Test;

public class RedeemAccessTokenSampleTest {
    @Test
    public void redeemAccessTokenTest() {
        try (TokenIO tokenIO = createClient()) {
            Member grantor = createMemberAndLinkAccounts(tokenIO);
            Alias granteeAlias = randomAlias();
            Member grantee = tokenIO.createMember(granteeAlias);

            Token token = createAccessToken(grantor, granteeAlias);

            Money balance0 = redeemAccessToken(grantee, token.getId());
            assertThat(MoneyUtil.parseAmount(balance0.getValue())).isGreaterThan(BigDecimal.TEN);
        }
    }
}
