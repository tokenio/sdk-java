package io.token.sample;

import static io.token.Token.TokenCluster.DEVELOPMENT;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;

import org.junit.Test;

public class LinkMemberAndBankSampleTest {
    @Test
    public void linkMemberAndBankTest() {
        Member member = LinkMemberAndBankSample.linkBank(DEVELOPMENT);
        assertThat(member.getAccounts().isEmpty()).isFalse();
    }
}
