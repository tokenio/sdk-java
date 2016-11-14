package io.token;

import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.rpc.client.RpcChannelFactory;
import io.token.security.SecretKey;

import java.util.List;

import static java.lang.String.format;

/**
 * Main entry point to the Token SDK. Use {@link io.token.TokenIO.Builder}
 * class to create an instance of the {@link TokenIOAsync} or {@link TokenIO}.
 * <p>
 * <p>
 * The class provides synchronous API with {@link TokenIOAsync} providing an
 * asynchronous version. {@link TokenIOAsync} instance can be obtained by
 * calling {@link #async} method.
 * </p>
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
     * Checks if a given username already exists.
     *
     * @param username username to check
     * @return {@code true} if username exists, {@code false} otherwise
     */
    public boolean usernameExists(String username) {
        return async.usernameExists(username).toBlocking().single();
    }

    /**
     * Creates a new Token member with a pair of auto generated keys and the
     * given username.
     *
     * @param username member username to use, must be unique
     * @return newly created member
     */
    public Member createMember(String username) {
        return async.createMember(username)
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

    /**
     * Logs in an existing member to the system, using the username
     *
     * @param username member id
     * @param key secret/public key pair to use
     * @return logged in member
     */
    public Member loginWithUsername(String username, SecretKey key) {
        return async.loginWithUsername(username, key)
                .map(MemberAsync::sync)
                .toBlocking()
                .single();
    }

    /**
     * Notifies to link accounts
     *
     * @param username username to notify
     * @param bankId bank ID to link
     * @param bankName bank name to link
     * @param accountLinkPayloads a list of account payloads to be linked
     */
    public NotifyStatus notifyLinkAccounts(
            String username,
            String bankId,
            String bankName,
            List<String> accountLinkPayloads) {
        return async.notifyLinkAccounts(username, bankId, bankName, accountLinkPayloads)
                .toBlocking()
                .single();
    }

    /**
     * Notifies to add a key
     *
     * @param username username to notify
     * @param publicKey public key to add
     * @return status of the notification
     */
    public NotifyStatus notifyAddKey(String username, byte[] publicKey, String name) {
        return async.notifyAddKey(username, publicKey, name)
                .toBlocking()
                .single();
    }

    /**
     * Notifies to link accounts and add a key
     *
     * @param username username to notify
     * @param bankId bank ID to link
     * @param bankName bank name to link
     * @param accountLinkPayloads a list of account payloads to be linked
     * @param publicKey public key to add
     * @return status of the notification
     */
    public NotifyStatus notifyLinkAccountsAndAddKey(
            String username,
            String bankId,
            String bankName,
            List<String> accountLinkPayloads,
            byte[] publicKey,
            String name) {
        return async.notifyLinkAccountsAndAddKey(
                username,
                bankId,
                bankName,
                accountLinkPayloads,
                publicKey,
                name)
                .toBlocking()
                .single();
    }
}
