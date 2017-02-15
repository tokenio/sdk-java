/**
 * Copyright (c) 2017 Token, Inc.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.token;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

import io.grpc.Metadata;
import io.token.gradle.TokenVersion;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.security.SecurityProtos.SealedMessage;
import io.token.rpc.client.RpcChannelFactory;
import io.token.security.CryptoEngineFactory;
import io.token.security.InMemoryKeyStore;
import io.token.security.KeyStore;
import io.token.security.TokenCryptoEngineFactory;

import java.io.Closeable;
import java.time.Duration;
import java.util.List;

/**
 * Main entry point to the Token SDK. Use {@link Token.Builder}
 * class to create an instance of the {@link TokenAsync} or {@link Token}.
 *
 * <p>The class provides synchronous API with {@link TokenAsync} providing an
 * asynchronous version. {@link TokenAsync} instance can be obtained by
 * calling {@link #async} method.
 */
public final class Token implements Closeable {
    private final TokenAsync async;

    /**
     * Creates an instance of Token.
     *
     * @param async real implementation that the calls are delegated to
     */
    Token(TokenAsync async) {
        this.async = async;
    }

    @Override
    public void close() {
        async.close();
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
     * Provisions a new device for an existing user. The call generates a set
     * of keys that are returned back. The keys need to be approved by an
     * existing device/keys.
     *
     * @param username member id to provision the device for
     * @return device information
     */
    public DeviceInfo provisionDevice(String username) {
        return async.provisionDevice(username)
                .toBlocking()
                .single();
    }

    /**
     * Logs in an existing member to the system.
     *
     * @param memberId member id
     * @return logged in member
     */
    public Member login(String memberId) {
        return async.login(memberId)
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
     * @return status of the notification request
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
     * @param name device/client name, e.g. iPhone, Chrome Browser, etc
     * @param key key that needs an approval
     * @return status of the notification request
     */
    public NotifyStatus notifyAddKey(
            String username,
            String name,
            Key key) {
        return async.notifyAddKey(
                username,
                name,
                key).toBlocking().single();
    }

    /**
     * Notifies to link accounts and add a key.
     *
     * @param username username to notify
     * @param bankId bank ID to link
     * @param bankName bank name to link
     * @param accountLinkPayloads a list of account payloads to be linked
     * @param name device/client name, e.g. iPhone, Chrome Browser, etc
     * @param key key that needs an approval
     * @return status of the notification request
     */
    public NotifyStatus notifyLinkAccountsAndAddKey(
            String username,
            String bankId,
            String bankName,
            List<SealedMessage> accountLinkPayloads,
            String name,
            Key key) {
        return async.notifyLinkAccountsAndAddKey(
                username,
                bankId,
                bankName,
                accountLinkPayloads,
                name,
                key).toBlocking().single();
    }

    /**
     * Defines Token cluster to connect to.
     */
    public enum TokenCluster {
        PRODUCTION("api-grpc.token.io"),
        STAGING("api-grpc.stg.token.io"),
        DEVELOPMENT("api-grpc.dev.token.io");

        private final String envUrl;

        TokenCluster(String url) {
            this.envUrl = url;
        }

        public String url() {
            return envUrl;
        }
    }

    /**
     * Used to create a new {@link Token} instances.
     */
    public static final class Builder {
        private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
        private static final int DEFAULT_SSL_PORT = 443;

        private int port;
        private boolean useSsl;
        private String hostName;
        private Duration timeout;
        private KeyStore keyStore;

        /**
         * Creates new builder instance with the defaults initialized.
         */
        public Builder() {
            this.timeout = DEFAULT_TIMEOUT;
            this.port = DEFAULT_SSL_PORT;
            this.useSsl = true;
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
            this.useSsl = port == DEFAULT_SSL_PORT;
            return this;
        }

        /**
         * Sets Token cluster to connect to.
         *
         * @param cluster {@link TokenCluster} instance.
         * @return this builder instance
         */
        public Builder connectTo(TokenCluster cluster) {
            this.hostName = cluster.url();
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
         * Sets the keystore to be used with the SDK.
         *
         * @param keyStore the keystore to be used
         * @return this builder instance
         */
        public Builder withKeyStore(KeyStore keyStore) {
            this.keyStore = keyStore;
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
            if (keyStore == null) {
                keyStore = new InMemoryKeyStore();
            }
            CryptoEngineFactory cryptoFactory = new TokenCryptoEngineFactory(keyStore);
            Metadata versionHeaders = new Metadata();
            versionHeaders.put(
                    Metadata.Key.of("token-sdk", ASCII_STRING_MARSHALLER),
                    "java");
            versionHeaders.put(
                    Metadata.Key.of("token-sdk-version", ASCII_STRING_MARSHALLER),
                    TokenVersion.getVersion());
            return new TokenAsync(
                    RpcChannelFactory
                            .builder(hostName, port, useSsl)
                            .withTimeout(timeout)
                            .withMetadata(versionHeaders)
                            .build(),
                    cryptoFactory);
        }
    }
}
