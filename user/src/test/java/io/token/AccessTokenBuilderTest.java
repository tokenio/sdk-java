package io.token;

import static io.token.proto.common.alias.AliasProtos.Alias.Type.EMAIL;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos;
import io.token.user.AccessTokenBuilder;

import org.junit.Test;

public class AccessTokenBuilderTest {
    private static Alias alias = Alias.newBuilder()
            .setValue("alias@token.io")
            .setType(EMAIL)
            .build();

    @Test(expected = IllegalArgumentException.class)
    public void throwsOnMissingFrom() {
        AccessTokenBuilder.create(alias).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsOnMissingResource() {
        AccessTokenBuilder
                .create(alias)
                .from("member")
                .build();
    }

    @Test
    public void allValidationsPass() {
        TokenProtos.TokenPayload payload = AccessTokenBuilder
                .create(alias)
                .from("member")
                .forAccount("12345678")
                .build();
        assertThat(payload).isNotNull();
    }
}
