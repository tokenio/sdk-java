package io.token.sample;

import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.TokenIO;

import org.junit.Test;

public class LinkMemberAndBankSampleTest {
    @Test
    public void linkMemberAndBankTest() {
        try (TokenIO tokenIO = createClient()) {
            Member member = tokenIO.createMember(randomAlias());

            LinkMemberAndBankSample.linkBankAccounts(member);
            assertThat(member.getAccounts().isEmpty()).isFalse();
        }
    }
}
