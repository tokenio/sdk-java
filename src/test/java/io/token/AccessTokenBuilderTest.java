package io.token;

import io.token.proto.common.token.TokenProtos;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
                AccessTokenBuilder.create("username").from("member").toAllAccounts().build();
        assertThat(payload).isNotNull();
    }
}
