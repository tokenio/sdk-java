package io.token.sample;

import static io.token.TokenIO.TokenCluster.DEVELOPMENT;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;

import org.junit.Test;

public class CreateMemberSampleTest {
    @Test
    public void createMemberTest() {
        Member member = CreateMemberSample.createMember(DEVELOPMENT);
        assertThat(member).isNotNull();
    }
}
