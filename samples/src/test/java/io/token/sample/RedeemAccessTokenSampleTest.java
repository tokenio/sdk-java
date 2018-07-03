package io.token.sample;

import static io.token.sample.CreateAndEndorseAccessTokenSample.createAccessToken;
import static io.token.sample.RedeemAccessTokenSample.carefullyUseAccessToken;

import static io.token.sample.RedeemAccessTokenSample.redeemAccessToken;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createMemberAndLinkAccounts;
import static io.token.sample.TestUtil.randomAlias;
import static io.token.sample.TestUtil.waitUntil;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.AccessTokenBuilder;
import io.token.Member;
import io.token.TokenIO;
import io.token.proto.MoneyUtil;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;

import java.math.BigDecimal;
import java.util.Arrays;

import org.junit.Test;

public class RedeemAccessTokenSampleTest {
    @Test
    public void redeemAccessTokenTest() {
        try (TokenIO tokenIO = createClient()) {
            Member grantor = createMemberAndLinkAccounts(tokenIO);
            Alias granteeAlias = randomAlias();
            Member grantee = tokenIO.createMember(granteeAlias);
            // wait until alias is processed by the asynchronous verification job (this is needed
            // only for +noverify aliases)
            waitUntil(() -> assertThat(grantee.aliases()).contains(granteeAlias));

            Token token = createAccessToken(grantor, granteeAlias);
            Money balance0 = redeemAccessToken(grantee, token.getId());
            assertThat(MoneyUtil.parseAmount(balance0.getValue())).isGreaterThan(BigDecimal.TEN);
        }
    }

    @Test
    public void useAccessTokenTest() {
        try (TokenIO tokenIO = createClient()) {
            Member grantor = tokenIO.createMember(randomAlias());
            // wait until alias is processed by the asynchronous verification job (this is needed
            // only for +noverify aliases)
            waitUntil(() -> assertThat(grantor.aliases()).isNotEmpty());
            final String account1Id = LinkMemberAndBankSample.linkBankAccounts(grantor).id();
            final String account2Id = LinkMemberAndBankSample.linkBankAccounts(grantor).id();
            LinkMemberAndBankSample.linkBankAccounts(grantor); // a third account

            Alias granteeAlias = randomAlias();
            Member grantee = tokenIO.createMember(granteeAlias);
            // wait until alias is processed by the asynchronous verification job (this is needed
            // only for +noverify aliases)
            waitUntil(() -> assertThat(grantee.aliases()).contains(granteeAlias));

            // get a token from doc sample code: all accounts, all balance
            Token token1 = createAccessToken(grantor, granteeAlias);

            Money balance1 = carefullyUseAccessToken(grantee, token1.getId());
            assertThat(MoneyUtil.parseAmount(balance1.getValue())).isGreaterThan(BigDecimal.TEN);

            TokenOperationResult replaceResult = grantor.replaceAndEndorseAccessToken(
                    token1,
                    AccessTokenBuilder.fromPayload(token1.getPayload())
                            .forAccount(account1Id)
                            .forAccount(account2Id)
                            .forAccountBalances(account1Id)
                            .forAccountBalances(account2Id));
            final Token token3 = replaceResult.getToken();

            Money balance3 = carefullyUseAccessToken(grantee, token1.getId()); // use replaced token
            assertThat(MoneyUtil.parseAmount(balance3.getValue())).isGreaterThan(BigDecimal.TEN);

            Money balance6 = carefullyUseAccessToken(grantee, token3.getId()); // use new token
            assertThat(MoneyUtil.parseAmount(balance6.getValue())).isGreaterThan(BigDecimal.TEN);
            grantor.unlinkAccounts(Arrays.asList(account1Id, account2Id));
            Money balance7 = carefullyUseAccessToken(grantee, token3.getId());
            assertThat(MoneyUtil.parseAmount(balance7.getValue())).isZero();
        }
    }
}
