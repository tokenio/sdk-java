package io.token.sample;

import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.banklink.Banklink;
import io.token.proto.common.money.MoneyProtos.Money;

import java.util.Map;

import org.junit.Test;

public class GetBalanceSampleTest {
    @Test
    public void memberGetBalanceSampleTest() {
        try (TokenIO tokenIO = createClient()) {
            Member member = tokenIO.createMember(randomAlias());
            Banklink.BankAuthorization encryptedBankAuthorization =
                    member.createTestBankAccount(1000.0, "EUR");
            member.linkAccounts(encryptedBankAuthorization);

            Map<String, Double> sums = GetBalanceSample.memberGetBalanceSample(member);
            assertThat(sums.get("EUR")).isEqualTo(1000.0);
        }
    }

    @Test
    public void accountGetBalanceSampleTest() {
        try (TokenIO tokenIO = createClient()) {
            Member member = tokenIO.createMember(randomAlias());
            Banklink.BankAuthorization encryptedBankAuthorization =
                    member.createTestBankAccount(1000.0, "EUR");
            member.linkAccounts(encryptedBankAuthorization);

            Map<String, Double> sums = GetBalanceSample.accountGetBalanceSample(member);
            assertThat(sums.get("EUR")).isEqualTo(1000.0);
        }
    }

    @Test
    public void memberGetBalanceMapSampleTest() {
        try (TokenIO tokenIO = createClient()) {
            Member member = tokenIO.createMember(randomAlias());
            Banklink.BankAuthorization encryptedBankAuthorizationA =
                    member.createTestBankAccount(1000.0, "EUR");
            member.linkAccounts(encryptedBankAuthorizationA);

            Banklink.BankAuthorization encryptedBankAuthorizationB =
                    member.createTestBankAccount(500.0, "EUR");

            member.linkAccounts(encryptedBankAuthorizationB);

            Map<String, Money> balances = GetBalanceSample.memberGetBalanceMapSample(member);
            assertThat(balances.size()).isEqualTo(2);
            assertThat(balances.values().stream().map(Money::getValue).map(Double::parseDouble))
                    .containsExactlyInAnyOrder(1000.0, 500.0);
        }
    }
}
