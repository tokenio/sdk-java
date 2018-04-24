package io.token;

import static io.token.proto.common.alias.AliasProtos.Alias.Type.EMAIL;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos;

import org.junit.Test;

public class AccessTokenBuilderTest {
    private static String TEST_REALM = "token-test";
    private static Alias alias = Alias.newBuilder()
            .setValue("alias@token.io")
            .setType(EMAIL)
            .build();

    @Test(expected = IllegalArgumentException.class)
    public void throwsOnMissingFrom() {
        AccessTokenBuilder.create(alias, TEST_REALM).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsOnMissingResource() {
        AccessTokenBuilder
                .create(alias, TEST_REALM)
                .from("member")
                .build();
    }

    @Test
    public void allValidationsPass() {
        TokenProtos.TokenPayload payload = AccessTokenBuilder
                .create(alias, TEST_REALM)
                .from("member")
                .forAllAccounts()
                .build();
        assertThat(payload).isNotNull();
    }
}
