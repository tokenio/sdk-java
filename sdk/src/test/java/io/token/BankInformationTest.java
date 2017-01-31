package io.token;

import static io.token.TokenRule.DEFAULT_BANK_ID;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;

public class BankInformationTest {
    @Rule
    public TokenRule rule = new TokenRule();
    private Member member = rule.member();

    @Test
    public void getBanks() {
        assertThat(member.getBanks()).isNotEmpty();
    }

    @Test
    public void getBankInfo() {
        assertThat(member.getBankInfo(DEFAULT_BANK_ID)).isNotNull();
    }
}
