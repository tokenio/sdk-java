package io.token.sample;

import static io.token.sample.RedeemAccessTokenSample.redeemAccessToken;
import static io.token.sample.TestUtil.createAccessToken;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createUserMember;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.proto.MoneyUtil;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;

import java.math.BigDecimal;

import org.junit.Test;

public class RedeemAccessTokenSampleTest {
    @Test
    public void redeemAccessTokenTest() {
        try (TokenClient tokenClient = createClient()) {
            io.token.user.Member grantor = createUserMember();
            String accountId = grantor.getAccountsBlocking().get(0).id();
            Alias granteeAlias = randomAlias();
            Member grantee = tokenClient.createMemberBlocking(granteeAlias);

            Token token = createAccessToken(grantor, accountId, granteeAlias);
            Money balance0 = redeemAccessToken(grantee, token.getId());
            assertThat(MoneyUtil.parseAmount(balance0.getValue())).isGreaterThan(BigDecimal.TEN);
        }
    }
}
