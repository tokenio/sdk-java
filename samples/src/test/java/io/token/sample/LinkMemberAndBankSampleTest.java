package io.token.sample;

import static io.token.proto.common.testing.Sample.alias;
import static io.token.TokenIO.TokenCluster.DEVELOPMENT;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.TokenIO;

import org.junit.Test;

public class LinkMemberAndBankSampleTest {
    @Test
    public void linkMemberAndBankTest() {
        try (TokenIO tokenIO = TokenIO.create(DEVELOPMENT)) {
            Member member = tokenIO.createMember(alias());

            LinkMemberAndBankSample.linkBankAccounts(member);
            assertThat(member.getAccounts().isEmpty()).isFalse();
        }
    }
}
