package io.token.sample;

import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;

import org.junit.Test;

public class LinkMemberAndBankSampleTest {
    @Test
    public void linkMemberAndBankTest() {
        Member member = LinkMemberAndBankSample
                .linkBank("api-grpc.dev.token.io", "fank-grpc.dev.token.io");
        assertThat(member.getAccounts().isEmpty()).isFalse();
    }
}
