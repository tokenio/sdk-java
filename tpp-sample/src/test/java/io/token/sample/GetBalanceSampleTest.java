package io.token.sample;

import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.transaction.TransactionProtos.Balance;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;

import java.util.List;
import java.util.Map;

import org.junit.Test;

public class GetBalanceSampleTest {
    @Test
    public void memberGetBalanceSampleTest() {
        try (TokenClient tokenClient = createClient()) {
            Member member = tokenClient.createMemberBlocking(randomAlias());
            member.createTestBankAccountBlocking(1000.0, "EUR");

            Map<String, Double> sums = GetBalanceSample.memberGetBalanceSample(member);
            assertThat(sums.get("EUR")).isEqualTo(1000.0);
        }
    }

    @Test
    public void accountGetBalanceSampleTest() {
        try (TokenClient tokenClient = createClient()) {
            Member member = tokenClient.createMemberBlocking(randomAlias());
            member.createTestBankAccountBlocking(1000.0, "EUR");

            Map<String, Double> sums = GetBalanceSample.accountGetBalanceSample(member);
            assertThat(sums.get("EUR")).isEqualTo(1000.0);
        }
    }

    @Test
    public void memberGetBalancesSampleTest() {
        try (TokenClient tokenClient = createClient()) {
            Member member = tokenClient.createMemberBlocking(randomAlias());
            member.createTestBankAccountBlocking(1000.0, "EUR");

            member.createTestBankAccountBlocking(500.0, "EUR");

            List<Balance> balances = GetBalanceSample.memberGetBalanceListSample(member);
            assertThat(balances.size()).isEqualTo(2);
            assertThat(balances
                    .stream()
                    .map(Balance::getCurrent)
                    .map(Money::getValue)
                    .map(Double::parseDouble))
                    .containsExactlyInAnyOrder(1000.0, 500.0);
        }
    }
}
