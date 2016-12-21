package io.token;

import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.security.SecurityProtos.SealedMessage;
import io.token.rpc.client.RpcChannelFactory;
import io.token.security.Signer;

import java.security.PublicKey;
import java.time.Duration;
import java.util.List;

/**
 * Main entry point to the Token SDK. Use {@link Token.Builder}
 * class to create an instance of the {@link TokenAsync} or {@link Token}.
 * <p>The class provides synchronous API with {@link TokenAsync} providing an
 * asynchronous version. {@link TokenAsync} instance can be obtained by
 * calling {@link #async} method.
 * </p>
 */
public final class Token {
    private final TokenAsync async;

    /**
     * Creates an instance of Token.
     *
     * @param async real implementation that the calls are delegated to
     */
    Token(TokenAsync async) {
        this.async = async;
    }

    /**
     * Creates a new {@link Builder} instance that is used to configure and
     * build a {@link Token} instance.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns an async version of the API.
     *
     * @return asynchronous version of the account API
     */
    public TokenAsync async() {
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
     * @param signer the signer to use
     * @return logged in member
     */
    public Member login(String memberId, Signer signer) {
        return async.login(memberId, signer)
                .map(MemberAsync::sync)
                .toBlocking()
                .single();
    }

    /**
     * Logs in an existing member to the system, using the username.
     *
     * @param username member id
     * @param signer the signer to use
     * @return logged in member
     */
    public Member loginWithUsername(String username, Signer signer) {
        return async.loginWithUsername(username, signer)
                .map(MemberAsync::sync)
                .toBlocking()
                .single();
    }

    /**
     * Notifies to link accounts.
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
            List<SealedMessage> accountLinkPayloads) {
        return async.notifyLinkAccounts(username, bankId, bankName, accountLinkPayloads)
                .toBlocking()
                .single();
    }

    /**
     * Notifies to add a key.
     *
     * @param username username to notify
     * @param publicKey public key to add
     * @return status of the notification
     */
    public NotifyStatus notifyAddKey(String username, PublicKey publicKey, String name) {
        return async.notifyAddKey(username, publicKey, name)
                .toBlocking()
                .single();
    }

    /**
     * Notifies to link accounts and add a key.
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
            List<SealedMessage> accountLinkPayloads,
            PublicKey publicKey,
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

    /**
     * Used to create a new {@link Token} instances.
     */
    public static final class Builder {
        private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

        private String hostName;
        private int port;
        private Duration timeout;

        /**
         * Creates new builder instance with the defaults initialized.
         */
        public Builder() {
            this.timeout = DEFAULT_TIMEOUT;
        }

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
         * Sets timeout that is used for the RPC calls.
         *
         * @param timeout RPC call timeout
         * @return this builder instance
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Builds and returns a new {@link Token} instance.
         *
         * @return {@link Token} instance
         */
        public Token build() {
            return buildAsync().sync();
        }

        /**
         * Builds and returns a new {@link TokenAsync} instance.
         *
         * @return {@link Token} instance
         */
        public TokenAsync buildAsync() {
            return new TokenAsync(RpcChannelFactory
                    .builder(hostName, port)
                    .withTimeout(timeout)
                    .build());
        }
    }
}
