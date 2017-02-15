package io.token.sample;

import static io.token.TokenIO.TokenCluster.DEVELOPMENT;

import io.token.Member;
import io.token.TokenIO;
import io.token.TokenIO.TokenCluster;

/**
 * Create a new Token member record.
 */
public final class CreateMemberSample {
    /**
     * Creates and returns a new token member.
     *
     * @param tokenCluster Token cluster to connect to (e.g.: TokenCluster.PRODUCTION)
     * @return a new Member instance
     */
    public static Member createMember(TokenCluster tokenCluster) {
        try (TokenIO tokenIO = TokenIO.create(DEVELOPMENT)) {
            return tokenIO.createMember(TestUtil.newUserName());
        }
    }
}
