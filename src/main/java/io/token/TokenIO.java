package io.token;

import io.grpc.ManagedChannel;
import io.token.rpc.Client;
import io.token.rpc.ClientFactory;
import io.token.rpc.UnauthenticatedClient;
import io.token.rpc.client.RpcChannelFactory;
import io.token.security.Crypto;
import io.token.security.SecretKey;
import rx.Observable;

import static java.lang.String.format;

/**
 * Main entry point to the Token SDK. Use this class to create an instance of
 * the {@link Token} and then use {@link #createMember} to create new member
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
         * Builds and returns a new {@link Token} instance.
         *
         * @return {@link TokenIO} instance
         */
        public TokenIO build() {
            return new TokenIO(RpcChannelFactory.forTarget(format("dns:///%s:%d/", hostName, port)));
        }
    }

    private final ManagedChannel channel;

    /**
     * Creates a new {@link Builder} instance that is used to configure and
     * build a {@link TokenIO} instance.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    private TokenIO(ManagedChannel channel) {
        this.channel = channel;
    }

    /**
     * Creates a new Token member with a pair of auto generated keys and the
     * given alias.
     *
     * @param alias member alias to use, must be unique
     * @return newly created member
     */
    public Member createMember(String alias) {
        return createMemberAsync(alias).toBlocking().single();
    }

    /**
     * Creates a new Token member with a pair of auto generated keys and the
     * given alias.
     *
     * @param alias member alias to use, must be unique
     * @return newly created member
     */
    public Observable<Member> createMemberAsync(String alias) {
        SecretKey key = Crypto.generateSecretKey();
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);

        return unauthenticated
                .createMemberId()
                .flatMap(memberId -> unauthenticated.addFirstKey(memberId, key))
                .flatMap(member -> {
                    Client client = ClientFactory.authenticated(channel, member.getId(), key);
                    return client
                            .addAlias(member, alias)
                            .map(m -> new Member(m, key, client));
                });
    }

    /**
     * Logs in an existing member to the system.
     *
     * @param memberId member id
     * @param key secret/public key pair to use
     * @return logged in member
     */
    public Member login(String memberId, SecretKey key) {
        return loginAsync(memberId, key).toBlocking().single();
    }

    /**
     * Logs in an existing member to the system.
     *
     * @param memberId member id
     * @param key secret/public key pair to use
     * @return logged in member
     */
    public Observable<Member> loginAsync(String memberId, SecretKey key) {
        Client client = ClientFactory.authenticated(channel, memberId, key);
        return client
                .getMember()
                .map(member -> new Member(member, key, client));
    }
}
