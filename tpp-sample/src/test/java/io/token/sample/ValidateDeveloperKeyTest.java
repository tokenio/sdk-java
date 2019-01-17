package io.token.sample;

import static io.token.TokenClient.TokenCluster.DEVELOPMENT;
import static io.token.sample.TestUtil.randomAlias;

import io.grpc.StatusRuntimeException;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;

import org.junit.Test;

public class ValidateDeveloperKeyTest {
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
