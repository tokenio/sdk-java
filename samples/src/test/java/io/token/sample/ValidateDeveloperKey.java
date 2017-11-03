package io.token.sample;

import static io.token.TokenIO.TokenCluster.DEVELOPMENT;
import static io.token.sample.TestUtil.randomAlias;

import io.grpc.StatusRuntimeException;
import io.token.Member;
import io.token.TokenIO;

import org.junit.Test;

public class ValidateDeveloperKey {
    @Test(expected = StatusRuntimeException.class)
    public void invalidKey() {
        TokenIO tokenIO = TokenIO.create(DEVELOPMENT, "0");
        Member member = tokenIO.createMember(randomAlias());
    }

    @Test(expected = StatusRuntimeException.class)
    public void noKeyProvided() {
        TokenIO tokenIO = TokenIO.create(DEVELOPMENT, null
        );
    }
}
