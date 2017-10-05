package io.token.sample;

import static io.token.TokenIO.TokenCluster.DEVELOPMENT;
import static io.token.sample.TestUtil.newAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.banklink.Banklink;

import java.util.Map;

import org.junit.Test;

public class GetBalanceSampleTest {
    @Test
    public void memberGetBalanceSampleTest() {
        try (TokenIO tokenIO = TokenIO.create(DEVELOPMENT, "devKey")) {
            Member member = tokenIO.createMember(newAlias());
            Banklink.BankAuthorization encryptedBankAuthorization =
                    member.createTestBankAccount(1000.0, "EUR");
            member.linkAccounts(encryptedBankAuthorization);

            Map<String, Double> sums = GetBalanceSample.memberGetBalanceSample(member);
            assertThat(sums.get("EUR")).isEqualTo(1000.0);
        }
    }

    @Test
    public void accountGetBalanceSampleTest() {
        try (TokenIO tokenIO = TokenIO.create(DEVELOPMENT, "devKey")) {
            Member member = tokenIO.createMember(newAlias());
            Banklink.BankAuthorization encryptedBankAuthorization =
                    member.createTestBankAccount(1000.0, "EUR");
            member.linkAccounts(encryptedBankAuthorization);

            Map<String, Double> sums = GetBalanceSample.accountGetBalanceSample(member);
            assertThat(sums.get("EUR")).isEqualTo(1000.0);
        }
    }
}
