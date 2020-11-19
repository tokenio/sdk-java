package io.token.sample;

import static io.token.sample.TestUtil.createClient;
import static org.assertj.core.api.Assertions.assertThat;


import io.token.user.Member;
import io.token.user.TokenClient;

import org.junit.Test;

public class CreateMemberSampleTest {

    @Test
    public void createMember() {
        try (TokenClient tokenClient = createClient()) {
            Member member = CreateMemberSample.createMember(tokenClient);
            assertThat(tokenClient.getMemberBlocking(member.memberId()).memberId())
                    .isEqualTo(member.memberId());
        }
    }
}
