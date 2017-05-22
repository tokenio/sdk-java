package io.token;

import static org.assertj.core.api.Assertions.assertThat;

import io.token.common.TokenRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class BankInformationTest {
    @Rule
    public TokenRule rule = new TokenRule();
    private Member member;

    @Before
    public void before() {
        this.member = rule.member();
    }

    @Test
    public void getBanks() {
        assertThat(member.getBanks()).isNotEmpty();
    }

    @Test
    public void getBankInfo() {
        assertThat(member.getBankInfo(rule.getBankId())).isNotNull();
    }
}
