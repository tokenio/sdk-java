package io.token;

import io.token.rpc.client.RpcChannelFactory;
import io.token.security.SecretKey;

import static java.lang.String.format;

/**
 * Main entry point to the Token SDK. Use this class to create an instance of
 * the {@link TokenIO} and then use {@link #createMember} to create new member
 * or {@link #login} to login an existing member.
 */
public final class TokenIO {
    /**
     * Used to create a new {@link TokenIO} instances.
     */
    public static final class Builder {
        private String hostName;
        private int port;

        /**
         * Sets the host name of the Token Gateway Service to connect to.
         *
         * @param hostName host name, e.g. 'api.token.io'
         * @return this builder instance
         */
        public Builder hostName(String hostName) {
            this.hostName = hostName;
            return this;
        }

        /**
         * Sets the port of the Token Gateway Service to connect to.
         *
         * @param port port number
         * @return this builder instance
         */
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * Builds and returns a new {@link TokenIO} instance.
         *
         * @return {@link TokenIO} instance
         */
        public TokenIO build() {
            return buildAsync().sync();
        }

        /**
         * Builds and returns a new {@link TokenIOAsync} instance.
         *
         * @return {@link TokenIO} instance
         */
        public TokenIOAsync buildAsync() {
            return new TokenIOAsync(RpcChannelFactory.forTarget(format("dns:///%s:%d/", hostName, port)));
        }
    }

    /**
     * Creates a new {@link Builder} instance that is used to configure and
     * build a {@link TokenIO} instance.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    private final TokenIOAsync async;

    /**
     * @param async real implementation that the calls are delegated to
     */
    TokenIO(TokenIOAsync async) {
        this.async = async;
    }

    /**
     * @return asynchronous version of the account API
     */
    public TokenIOAsync async() {
        return async;
    }

    /**
     * Creates a new Token member with a pair of auto generated keys and the
     * given alias.
     *
     * @param alias member alias to use, must be unique
     * @return newly created member
     */
    public Member createMember(String alias) {
        return async.createMember(alias)
                .map(MemberAsync::sync)
                .toBlocking()
                .single();
    }

    /**
     * Logs in an existing member to the system.
     *
     * @param memberId member id
     * @param key secret/public key pair to use
     * @return logged in member
     */
    public Member login(String memberId, SecretKey key) {
        return async.login(memberId, key)
                .map(MemberAsync::sync)
                .toBlocking()
                .single();
    }
}
