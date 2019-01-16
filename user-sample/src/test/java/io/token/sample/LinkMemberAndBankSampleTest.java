package io.token.sample;

import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.user.Member;
import io.token.user.TokenClient;

import org.junit.Test;

public class LinkMemberAndBankSampleTest {
    @Test
    public void linkMemberAndBankTest() {
        try (TokenClient tokenClient = createClient()) {
            Member member = tokenClient.createMemberBlocking(randomAlias());

            LinkMemberAndBankSample.linkBankAccounts(member);
            assertThat(member.getAccountsBlocking().isEmpty()).isFalse();
        }
    }
}
