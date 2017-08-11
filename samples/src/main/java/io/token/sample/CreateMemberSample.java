package io.token.sample;

import static io.token.TokenIO.TokenCluster.SANDBOX;
import static io.token.proto.common.testing.Sample.alias;

import io.token.Member;
import io.token.TokenIO;

/**
 * Create a new Token member record.
 */
public final class CreateMemberSample {
    /**
     * Creates and returns a new token member.
     *
     * @return a new Member instance
     */
    public static Member createMember() {
        try (TokenIO tokenIO = TokenIO.create(SANDBOX)) {
            return tokenIO.createMember(alias());
        }
    }
}
