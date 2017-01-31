package io.token.sample;

import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;

import org.junit.Test;

public class CreateMemberTest {
    @Test
    public void createMemberTest() {
        CreateMember createMemberSample = new CreateMember();
        Member member = createMemberSample.createMember();
        assertThat(member).isNotNull();
    }
}
