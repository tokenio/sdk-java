package io.token.sample;

import static io.token.TokenIO.TokenCluster.DEVELOPMENT;
import static io.token.common.Constants.DEV_KEY;
import static io.token.sample.TestUtil.newAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.TokenIO;

import org.junit.Test;

public class LinkMemberAndBankSampleTest {
    @Test
    public void linkMemberAndBankTest() {
        try (TokenIO tokenIO = TokenIO.create(DEVELOPMENT, DEV_KEY)) {
            Member member = tokenIO.createMember(newAlias());

            LinkMemberAndBankSample.linkBankAccounts(member);
            assertThat(member.getAccounts().isEmpty()).isFalse();
        }
    }
}
