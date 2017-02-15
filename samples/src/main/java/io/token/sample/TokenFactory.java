package io.token.sample;

import io.token.TokenIO;
import io.token.TokenIO.TokenCluster;
import io.token.util.Util;

import java.time.Duration;

/**
 * A helper factory to initialize the SDK.
 */
public interface TokenFactory {
    /**
     * Initialize TokenIO SDK instance.
     *
     * @param tokenCluster Token cluster to connect to (e.g.: TokenCluster.PRODUCTION)
     * @return token SDK instance, call {@link TokenIO#close} when you are done
     */
    static TokenIO newSdk(TokenCluster tokenCluster) {
        return TokenIO.builder()
                .connectTo(tokenCluster)
                .timeout(Duration.ofSeconds(15))
                .build();
    }

    /**
     * Generates random user name to be used for testing.
     *
     * @return random user name
     */
    static String newUserName() {
        return "username-" + Util.generateNonce();
    }
}
