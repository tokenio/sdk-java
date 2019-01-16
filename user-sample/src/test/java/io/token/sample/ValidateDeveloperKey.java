package io.token.sample;

import static io.token.TokenClient.TokenCluster.DEVELOPMENT;
import static io.token.sample.TestUtil.randomAlias;

import io.grpc.StatusRuntimeException;
import io.token.user.Member;
import io.token.user.TokenClient;

import org.junit.Test;

public class ValidateDeveloperKey {
    @Test(expected = StatusRuntimeException.class)
    public void invalidKey() {
        TokenClient tokenClient = TokenClient.create(DEVELOPMENT, "0");
        Member member = tokenClient.createMemberBlocking(randomAlias());
    }

    @Test(expected = StatusRuntimeException.class)
    public void noKeyProvided() {
        TokenClient tokenIO = TokenClient.create(DEVELOPMENT, null);
    }
}
