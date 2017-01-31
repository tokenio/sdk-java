package io.token;

import static org.assertj.core.api.Assertions.assertThat;

import io.token.proto.common.token.TokenProtos;

import org.junit.Test;

public class AccessTokenBuilderTest {

    @Test(expected = IllegalArgumentException.class)
    public void throwsOnMissingFrom() {
        AccessTokenBuilder.create("username").build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsOnMissingResource() {
        AccessTokenBuilder.create("username").from("member").build();
    }

    @Test
    public void allValidationsPass() {
        TokenProtos.TokenPayload payload =
                AccessTokenBuilder.create("username").from("member").forAllAccounts().build();
        assertThat(payload).isNotNull();
    }
}
