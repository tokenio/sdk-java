package io.token.sample;

import com.google.common.net.HostAndPort;
import io.token.Member;
import io.token.Token;
import io.token.util.Util;

import java.time.Duration;

/**
 * Create a new Token member record.
 */
public final class CreateMemberSample {
    /**
     * Creates and returns a new token member.
     *
     * @param tokenApiUrl token API url (e.g.: "api-grpc.token.io")
     * @return a new Member instance
     */
    public static Member createMember(String tokenApiUrl) {
        // Initialize Token SDK instance.
        Token sdk = Token.builder()
                .hostName(HostAndPort.fromHost(tokenApiUrl).getHostText())
                .port(443)
                .timeout(Duration.ofSeconds(15))
                .useSsl(true)
                .build();

        // Create a member account with a random username.
        return sdk.createMember("username-" + Util.generateNonce());
    }
}
