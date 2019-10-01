package io.token.sample;

import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.sample.CreateAndEndorseAccessTokenSample.createBalanceAccessToken;
import static io.token.sample.CreateAndEndorseAccessTokenSample.createStandingOrdersAccessToken;
import static io.token.sample.CreateAndEndorseAccessTokenSample.createTransactionsAccessToken;
import static io.token.sample.CreateStandingOrderTokenSample.createStandingOrderToken;
import static io.token.sample.CreateTransferTokenSample.createTransferToken;
import static io.token.sample.RedeemAccessTokenSample.redeemBalanceAccessToken;
import static io.token.sample.RedeemAccessTokenSample.redeemStandingOrdersAccessToken;
import static io.token.sample.RedeemAccessTokenSample.redeemTransactionsAccessToken;
import static io.token.sample.RedeemStandingOrderTokenSample.redeemStandingOrderToken;
import static io.token.sample.RedeemTransferTokenSample.redeemTransferToken;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createUserMember;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Account;
import io.token.proto.MoneyUtil;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transaction.TransactionProtos.StandingOrder;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;

public class RedeemAccessTokenSampleTest {
    @Test
    public void redeemBalanceAccessTokenTest() {
        try (TokenClient tokenClient = createClient()) {
            io.token.user.Member grantor = createUserMember();
            String accountId = grantor.getAccountsBlocking().get(0).id();
            Alias granteeAlias = randomAlias();
            Member grantee = tokenClient.createMemberBlocking(granteeAlias);

            Token token = createBalanceAccessToken(grantor, accountId, granteeAlias);
            Money balance0 = redeemBalanceAccessToken(grantee, token.getId());
            assertThat(MoneyUtil.parseAmount(balance0.getValue())).isGreaterThan(BigDecimal.TEN);
        }
    }

    @Test
    public void redeemTransactionsAccessTokenTest() {
        try (TokenClient tokenClient = createClient()) {
            io.token.user.Member grantor = createUserMember();

            String accountId = grantor.getAccountsBlocking().get(0).id();
            Alias granteeAlias = randomAlias();
            Member grantee = tokenClient.createMemberBlocking(granteeAlias);

            // make some transactions
            Alias payeeAlias = randomAlias();
            Member payee = tokenClient.createMemberBlocking(payeeAlias);
            Account payeeAccount = payee.createTestBankAccountBlocking(1000, "EUR");
            for (int i = 0; i < 5; i++) {
                Token token = createTransferToken(grantor, payeeAlias, STANDARD);
                redeemTransferToken(
                        payee,
                        payeeAccount.id(),
                        token.getId());
            }

            Token token = createTransactionsAccessToken(grantor, accountId, granteeAlias);
            List<Transaction> transactions = redeemTransactionsAccessToken(grantee, token.getId());
            assertThat(transactions).hasSize(5);
        }
    }

    @Test
    public void redeemStandingOrdersAccessTokenTest() {
        try (TokenClient tokenClient = createClient()) {
            io.token.user.Member grantor = createUserMember();
            String accountId = grantor.getAccountsBlocking().get(0).id();
            Alias granteeAlias = randomAlias();
            Member grantee = tokenClient.createMemberBlocking(granteeAlias);

            // make some standing orders
            Alias payeeAlias = randomAlias();
            Member payee = tokenClient.createMemberBlocking(payeeAlias);
            for (int i = 0; i < 10; i++) {
                Token token = createStandingOrderToken(grantor, payeeAlias, STANDARD);
                redeemStandingOrderToken(payee, token.getId());
            }

            Token token = createStandingOrdersAccessToken(grantor, accountId, granteeAlias);
            List<StandingOrder> standingOrders = redeemStandingOrdersAccessToken(
                    grantee,
                    token.getId());
            assertThat(standingOrders).hasSize(5);
        }
    }
}
