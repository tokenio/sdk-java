package io.token.sample;

import io.token.Member;
import io.token.Token;
import io.token.Token.TokenCluster;
import io.token.util.Util;

import java.time.Duration;

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
        // Initialize Token SDK instance.
        Token sdk = Token.builder()
                .connectTo(tokenCluster)
                .timeout(Duration.ofSeconds(15))
                .build();

        // Create a member account with a random username.
        return sdk.createMember("username-" + Util.generateNonce());
    }
}
