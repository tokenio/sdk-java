package io.token.sample;

import static io.token.TokenIO.TokenCluster.DEVELOPMENT;
import static io.token.common.Constants.devKey;
import static io.token.sample.TestUtil.newAlias;

import io.grpc.StatusRuntimeException;
import io.token.Member;
import io.token.TokenIO;

import org.junit.Test;

public class ValidateDeveloperKey {
    @Test
    public void validKey() {
        try (TokenIO tokenIO = TokenIO.create(
                DEVELOPMENT,
                devKey)) {
            Member member = tokenIO.createMember(newAlias());
        }
    }

    @Test(expected = StatusRuntimeException.class)
    public void invalidKey() {
        TokenIO tokenIO = TokenIO.create(
                DEVELOPMENT,
                "0");
        Member member = tokenIO.createMember(newAlias());
    }

    @Test(expected = StatusRuntimeException.class)
    public void noKeyProvided() {
        TokenIO tokenIO = TokenIO.create(
                DEVELOPMENT,
                null
        );
    }
}
