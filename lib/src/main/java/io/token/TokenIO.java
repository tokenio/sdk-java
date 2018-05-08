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
import static io.grpc.Status.INVALID_ARGUMENT;
import static io.token.TokenIO.TokenCluster.SANDBOX;

import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.reactivex.functions.Function;
import io.token.browser.BrowserFactory;
import io.token.gradle.TokenVersion;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.bank.BankProtos.Bank;
import io.token.proto.common.member.MemberProtos.MemberRecoveryOperation;
import io.token.proto.common.member.MemberProtos.MemberRecoveryOperation.Authorization;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.rpc.client.RpcChannelFactory;
import io.token.security.CryptoEngine;
import io.token.security.CryptoEngineFactory;
import io.token.security.InMemoryKeyStore;
import io.token.security.KeyStore;
import io.token.security.TokenCryptoEngineFactory;

import java.io.Closeable;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Main entry point to the Token SDK. Use {@link TokenIO.Builder}
 * class to create an instance of the {@link TokenIOAsync} or {@link TokenIO}.
 *
 * <p>The class provides synchronous API with {@link TokenIOAsync} providing an
 * asynchronous version. {@link TokenIOAsync} instance can be obtained by
 * calling {@link #async} method.</p>
 */
public class TokenIO implements Closeable {
    private final TokenIOAsync async;
    private final String devKey;

    /**
     * Creates an instance of Token SDK.
     *
     * @param async real implementation that the calls are delegated to
     * @param developerKey developer key
     */
    TokenIO(TokenIOAsync async, String developerKey) {
        this.async = async;
        this.devKey = developerKey;
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

    /**
     * Creates a new instance of {@link TokenIO} that's configured to use
     * the specified environment.
     *
     * @param cluster token cluster to connect to
     * @param developerKey developer key
     * @return {@link TokenIO} instance
     */
    public static TokenIO create(TokenCluster cluster, String developerKey) {
        return TokenIO.builder()
                .connectTo(cluster)
                .devKey(developerKey)
                .build();
    }

    /**
     * Creates a new instance of {@link TokenIOAsync} that's configured to use
     * the specified environment.
     *
     * @param cluster token cluster to connect to
     * @param developerKey developer key
     * @return {@link TokenIOAsync} instance
     */
    public static TokenIOAsync createAsync(TokenCluster cluster, String developerKey) {
        return TokenIO.builder()
                .connectTo(cluster)
                .devKey(developerKey)
                .buildAsync();
    }

    @Override
    public void close() {
        async.close();
    }

    /**
     * Returns an async version of the API.
     *
     * @return asynchronous version of the account API
     */
    public TokenIOAsync async() {
        return async;
    }

    /**
     * Checks if a given alias already exists.
     *
     * @param alias alias to check
     * @return {@code true} if alias exists, {@code false} otherwise
     */
    public boolean aliasExists(Alias alias) {
        return async.aliasExists(alias).blockingSingle();
    }

    /**
     * Checks if a given alias already exists.
     *
     * @param alias alias to check
     * @param realm realm of the alias
     * @return {@code true} if alias exists, {@code false} otherwise
     */
    public boolean aliasExists(Alias alias, String realm) {
        return async.aliasExists(alias, realm).blockingSingle();
    }

    /**
     * Looks up member id for a given alias.
     *
     * @param alias alias to check
     * @return member id, or throws exception if member not found
     */
    public String getMemberId(Alias alias) {
        return async.getMemberId(alias).blockingSingle();
    }

    /**
     * Looks up member id for a given alias.
     *
     * @param alias alias to check
     * @param realm realm of the alias
     * @return member id, or throws exception if member not found
     */
    public String getMemberId(Alias alias, String realm) {
        return async.getMemberId(alias, realm).blockingSingle();
    }

    /**
     * Creates a new business-use Token member with a set of auto-generated keys and alias.
     *
     * @param alias alias to associated with member
     * @return newly created member
     */
    public Member createBusinessMember(Alias alias) {
        return async.createBusinessMember(alias)
                .map(new MemberFunction())
                .blockingSingle();
    }

    /**
     * Creates a new business-use Token member with a set of auto-generated keys, an alias
     * and a realm.
     *
     * @param alias alias to associated with member
     * @param realm realm
     * @return newly created member
     */
    public Member createBusinessMember(Alias alias, String realm) {
        return async.createBusinessMember(alias, realm)
                .map(new MemberFunction())
                .blockingSingle();
    }

    /**
     * Creates a new personal-use Token member with a set of auto generated keys and the
     * given alias.
     *
     * @param alias member alias to use, must be unique
     * @return newly created member
     */
    public Member createMember(Alias alias) {
        return async.createMember(alias)
                .map(new MemberFunction())
                .blockingSingle();
    }

    /**
     * Creates a new personal-use Token member with a set of auto generated keys, the
     * given alias and a realm.
     *
     * @param alias member alias to use, must be unique
     * @param realm realm
     * @return newly created member
     */
    public Member createMember(Alias alias, String realm) {
        return async.createMember(alias, realm)
                .map(new MemberFunction())
                .blockingSingle();
    }

    /**
     * Creates a new personal-use Token member with a set of auto generated keys and no alias.
     *
     * @return newly created member
     */
    public Member createMember() {
        return async.createMember()
                .map(new MemberFunction())
                .blockingSingle();
    }

    /**
     * Provisions a new device for an existing user. The call generates a set
     * of keys that are returned back. The keys need to be approved by an
     * existing device/keys.
     *
     * @param alias member id to provision the device for
     * @return device information
     */
    public DeviceInfo provisionDevice(Alias alias) {
        return async.provisionDevice(alias)
                .blockingSingle();
    }

    /**
     * Provisions a new device for an existing user. The call generates a set
     * of keys that are returned back. The keys need to be approved by an
     * existing device/keys.
     *
     * @param alias member id to provision the device for
     * @param realm realm of the alias
     * @return device information
     */
    public DeviceInfo provisionDevice(Alias alias, String realm) {
        return async.provisionDevice(alias, realm)
                .blockingSingle();
    }

    /**
     * Return a MemberAsync set up to use some Token member's keys (assuming we have them).
     *
     * @param memberId member id
     * @return member
     */
    public Member getMember(String memberId) {
        return async.getMember(memberId)
                .map(new MemberFunction())
                .blockingSingle();
    }

    /**
     * Returns a token request for a specified token request id.
     *
     * @param requestId request id
     * @return token request
     */
    public TokenRequest retrieveTokenRequest(String requestId) {
        return async.retrieveTokenRequest(requestId).blockingSingle();
    }

    /**
     * Return a MemberAsync set up to use some Token member's keys (assuming we have them).
     *
     * @param memberId member id
     * @return member
     * @deprecated login's name changed to getMember
     */
    @Deprecated
    public Member login(String memberId) {
        return async.getMember(memberId)
                .map(new MemberFunction())
                .blockingSingle();
    }

    /**
     * Notifies to link accounts.
     *
     * @param alias alias to notify
     * @param authorization the bank authorization for the funding account
     * @return status of the notification request
     */
    public NotifyStatus notifyLinkAccounts(Alias alias, BankAuthorization authorization) {
        return async.notifyLinkAccounts(alias, authorization)
                .blockingSingle();
    }

    /**
     * Notifies to link accounts.
     *
     * @param alias alias to notify
     * @param realm realm of the alias
     * @param authorization the bank authorization for the funding account
     * @return status of the notification request
     */
    public NotifyStatus notifyLinkAccounts(
            Alias alias,
            String realm,
            BankAuthorization authorization) {
        return async.notifyLinkAccounts(alias, realm, authorization)
                .blockingSingle();
    }

    /**
     * Notifies to add a key.
     *
     * @param alias alias to notify
     * @param name device/client name, e.g. iPhone, Chrome Browser, etc
     * @param key key that needs an approval
     * @return status of the notification request
     */
    public NotifyStatus notifyAddKey(Alias alias, String name, Key key) {
        return async.notifyAddKey(
                alias,
                name,
                key).blockingSingle();
    }

    /**
     * Notifies to add a key.
     *
     * @param alias alias to notify
     * @param realm realm of the alias
     * @param name device/client name, e.g. iPhone, Chrome Browser, etc
     * @param key key that needs an approval
     * @return status of the notification request
     */
    public NotifyStatus notifyAddKey(
            Alias alias,
            String realm,
            String name,
            Key key) {
        return async.notifyAddKey(
                alias,
                realm,
                name,
                key).blockingSingle();
    }

    /**
     * Notifies to link accounts and add a key.
     *
     * @param alias alias to notify
     * @param authorization the bank authorization for the funding account
     * @param name device/client name, e.g. iPhone, Chrome Browser, etc
     * @param key key that needs an approval
     * @return status of the notification request
     */
    public NotifyStatus notifyLinkAccountsAndAddKey(
            Alias alias,
            BankAuthorization authorization,
            String name,
            Key key) {
        return async.notifyLinkAccountsAndAddKey(
                alias,
                authorization,
                name,
                key).blockingSingle();
    }

    /**
     * Notifies to link accounts and add a key.
     *
     * @param alias alias to notify
     * @param realm realm of the alias
     * @param authorization the bank authorization for the funding account
     * @param name device/client name, e.g. iPhone, Chrome Browser, etc
     * @param key key that needs an approval
     * @return status of the notification request
     */
    public NotifyStatus notifyLinkAccountsAndAddKey(
            Alias alias,
            String realm,
            BankAuthorization authorization,
            String name,
            Key key) {
        return async.notifyLinkAccountsAndAddKey(
                alias,
                realm,
                authorization,
                name,
                key).blockingSingle();
    }

    /**
     * Sends a notification to request a payment.
     *
     * @param tokenPayload the payload of a token to be sent
     * @return status of the notification request
     */
    public NotifyStatus notifyPaymentRequest(TokenPayload tokenPayload) {
        return async
                .notifyPaymentRequest(tokenPayload)
                .blockingSingle();
    }

    /**
     * Begins account recovery.
     *
     * @param alias the alias used to recover
     * @return the verification id
     */
    public String beginRecovery(Alias alias) {
        return async.beginRecovery(alias).blockingSingle();
    }

    /**
     * Begins account recovery.
     *
     * @param alias the alias used to recover
     * @param realm realm of the alias
     * @return the verification id
     */
    public String beginRecovery(Alias alias, String realm ) {
        return async.beginRecovery(alias, realm).blockingSingle();
    }

    /**
     * Create a recovery authorization for some agent to sign.
     *
     * @param memberId Id of member we claim to be.
     * @param privilegedKey new privileged key we want to use.
     * @return authorization structure for agent to sign
     */
    public Authorization createRecoveryAuthorization(String memberId, Key privilegedKey) {
        return async.createRecoveryAuthorization(memberId, privilegedKey).blockingSingle();
    }

    /**
     * Gets recovery authorization from Token.
     *
     * @param verificationId the verification id
     * @param code the code
     * @param key the privileged key
     * @return the member recovery operation
     */
    public MemberRecoveryOperation getRecoveryAuthorization(
            String verificationId,
            String code,
            Key key) {
        return async.getRecoveryAuthorization(verificationId, code, key).blockingSingle();
    }

    /**
     * Completes account recovery.
     *
     * @param memberId the member id
     * @param recoveryOperations the member recovery operations
     * @param privilegedKey the privileged public key in the member recovery operations
     * @param cryptoEngine the new crypto engine
     * @return the new member
     */
    public Member completeRecovery(
            String memberId,
            List<MemberRecoveryOperation> recoveryOperations,
            Key privilegedKey,
            CryptoEngine cryptoEngine) {
        return async.completeRecovery(memberId, recoveryOperations, privilegedKey, cryptoEngine)
                .map(new MemberFunction())
                .blockingSingle();
    }

    /**
     * Completes account recovery if the default recovery rule was set.
     *
     * @param memberId the member id
     * @param verificationId the verification id
     * @param code the code
     * @return the new member
     */
    public Member completeRecoveryWithDefaultRule(
            String memberId,
            String verificationId,
            String code) {
        return async.completeRecoveryWithDefaultRule(memberId, verificationId, code)
                .map(new MemberFunction())
                .blockingSingle();
    }

    /**
     * Returns a list of token enabled banks.
     *
     * @param bankIds If specified, return banks whose 'id' matches any one of the given ids
     *     (case-insensitive). Can be at most 1000.
     * @param page Result page to retrieve. Default to 1 if not specified.
     * @param perPage Maximum number of records per page. Can be at most 200. Default to 200
     *     if not specified.
     * @return a list of banks
     */
    public List<Bank> getBanks(
            @Nullable List<String> bankIds,
            @Nullable Integer page,
            @Nullable Integer perPage) {
        return getBanks(bankIds, null, null, page, perPage, null);
    }

    /**
     * Returns a list of token enabled banks.
     *
     * @param search If specified, return banks whose 'name' or 'identifier' contains the given
     *     search string (case-insensitive)
     * @param country If specified, return banks whose 'country' matches the given ISO 3166-1
     *     alpha-2 country code (case-insensitive)
     * @param page Result page to retrieve. Default to 1 if not specified.
     * @param perPage Maximum number of records per page. Can be at most 200. Default to 200
     *     if not specified.
     * @param sort The key to sort the results. Could be one of: name, provider and country.
     *     Defaults to name if not specified.
     * @return a list of banks
     */
    public List<Bank> getBanks(
            @Nullable String search,
            @Nullable String country,
            @Nullable Integer page,
            @Nullable Integer perPage,
            @Nullable String sort) {
        return getBanks(null, search, country, page, perPage, sort);
    }

    /**
     * Returns a list of token enabled banks.
     *
     * @param bankIds If specified, return banks whose 'id' matches any one of the given ids
     *     (case-insensitive). Can be at most 1000.
     * @param search If specified, return banks whose 'name' or 'identifier' contains the given
     *     search string (case-insensitive)
     * @param country If specified, return banks whose 'country' matches the given ISO 3166-1
     *     alpha-2 country code (case-insensitive)
     * @param page Result page to retrieve. Default to 1 if not specified.
     * @param perPage Maximum number of records per page. Can be at most 200. Default to 200
     *     if not specified.
     * @param sort The key to sort the results. Could be one of: name, provider and country.
     *     Defaults to name if not specified.
     * @return a list of banks
     */
    public List<Bank> getBanks(
            @Nullable List<String> bankIds,
            @Nullable String search,
            @Nullable String country,
            @Nullable Integer page,
            @Nullable Integer perPage,
            @Nullable String sort) {
        return async.getBanks(bankIds, search, country, page, perPage, sort).blockingSingle();
    }

    /**
     * Generate a Token request URL from a request ID, and state. This does not set a CSRF token
     * or pass in a state.
     *
     * @param requestId request id
     * @return token request url
     */
    public String generateTokenRequestUrl(String requestId) {
        return async.generateTokenRequestUrl(requestId).blockingSingle();
    }

    /**
     * Generate a Token request URL from a request ID, and state. This does not set a CSRF token.
     *
     * @param requestId request id
     * @param state state
     * @return token request url
     */
    public String generateTokenRequestUrl(String requestId, String state) {
        return async.generateTokenRequestUrl(requestId, state).blockingSingle();
    }

    /**
     * Generate a Token request URL from a request ID, an original state and a CSRF token.
     *
     * @param requestId request id
     * @param state state
     * @param csrfToken csrf token
     * @return token request url
     */
    public String generateTokenRequestUrl(String requestId, String state, String csrfToken) {
        return async.generateTokenRequestUrl(requestId, state, csrfToken).blockingSingle();
    }

    /**
     * Parse the token request callback URL to extract the state and the token ID. This assumes
     * that no CSRF token was set.
     *
     * @param callbackUrl token request callback url
     * @return TokenRequestCallback object containing the token id and the original state
     */
    public TokenRequestCallback parseTokenRequestCallbackUrl(final String callbackUrl) {
        return async.parseTokenRequestCallbackUrl(callbackUrl).blockingSingle();
    }

    /**
     * Parse the token request callback URL to extract the state and the token ID. Verify that the
     * state contains the CSRF token hash and that the signature on the state and CSRF token is
     * valid.
     *
     * @param callbackUrl token request callback url
     * @param csrfToken csrf token
     * @return TokenRequestCallback object containing the token id and the original state
     */
    public TokenRequestCallback parseTokenRequestCallbackUrl(String callbackUrl, String csrfToken) {
        return async
                .parseTokenRequestCallbackUrl(callbackUrl, csrfToken)
                .blockingSingle();
    }

    /**
     * Get a token ID based on a token's tokenRequestId.
     *
     * @param tokenRequestId token request id
     * @return token id
     */
    public String getTokenId(String tokenRequestId) {
        return async.getTokenId(tokenRequestId).blockingSingle();
    }

    /**
     * Defines Token cluster to connect to.
     */
    public enum TokenCluster {
        PRODUCTION("api-grpc.token.io", "web-app.token.io"),
        INTEGRATION("api-grpc.int.token.io", "web-app.int.token.io"),
        SANDBOX("api-grpc.sandbox.token.io", "web-app.sandbox.token.io"),
        STAGING("api-grpc.stg.token.io", "web-app.stg.token.io"),
        PERFORMANCE("api-grpc.perf.token.io", "web-app.perf.token.io"),
        DEVELOPMENT("api-grpc.dev.token.io", "web-app.dev.token.io");

        private final String envUrl;
        private String webAppUrl;

        TokenCluster(String url, String webAppUrl) {
            this.envUrl = url;
            this.webAppUrl = webAppUrl;
        }

        public String url() {
            return envUrl;
        }

        public String webAppUrl() {
            return webAppUrl;
        }
    }

    /**
     * Used to create a new {@link TokenIO} instances.
     */
    public static final class Builder {
        private static final long DEFAULT_TIMEOUT_MS = 10_000L;
        private static final int DEFAULT_SSL_PORT = 443;

        private int port;
        private boolean useSsl;
        private TokenCluster tokenCluster;
        private String hostName;
        private long timeoutMs;
        private CryptoEngineFactory cryptoEngine;
        private String devKey;
        private BrowserFactory browserFactory;

        /**
         * Creates new builder instance with the defaults initialized.
         */
        public Builder() {
            this.timeoutMs = DEFAULT_TIMEOUT_MS;
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
            this.tokenCluster = cluster;
            this.hostName = cluster.url();
            return this;
        }

        /**
         * Sets timeoutMs that is used for the RPC calls.
         *
         * @param timeoutMs RPC call timeoutMs
         * @return this builder instance
         */
        public Builder timeout(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        /**
         * Sets the keystore to be used with the SDK.
         *
         * @param keyStore the keystore to be used
         * @return this builder instance
         */
        public Builder withKeyStore(KeyStore keyStore) {
            this.cryptoEngine = new TokenCryptoEngineFactory(keyStore);
            return this;
        }

        /**
         * Sets the crypto engine to be used with the SDK.
         *
         * @param cryptoEngineFactory the crypto engine factory to use
         * @return this builder instance
         */
        public Builder withCryptoEngine(CryptoEngineFactory cryptoEngineFactory) {
            this.cryptoEngine = cryptoEngineFactory;
            return this;
        }

        /**
         * Sets the developer key to be used with the SDK.
         *
         * @param devKey developer key
         * @return this builder instance
         */
        public Builder devKey(String devKey) {
            this.devKey = devKey;
            return this;
        }

        /**
         * Sets the browser factory to be used with the SDK.
         *
         * @param browserFactory browser factory
         * @return this builder instance
         */
        public Builder withBrowserFactory(BrowserFactory browserFactory) {
            this.browserFactory = browserFactory;
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
            if (devKey == null || devKey.isEmpty()) {
                throw new StatusRuntimeException(INVALID_ARGUMENT
                        .withDescription("Please provide a developer key."
                                + " Contact Token for more details."));
            }

            Metadata headers = new Metadata();
            headers.put(
                    Metadata.Key.of("token-sdk", ASCII_STRING_MARSHALLER),
                    "java");
            headers.put(
                    Metadata.Key.of("token-sdk-version", ASCII_STRING_MARSHALLER),
                    TokenVersion.getVersion());
            headers.put(
                    Metadata.Key.of("token-dev-key", ASCII_STRING_MARSHALLER),
                    devKey);
            return new TokenIOAsync(
                    RpcChannelFactory
                            .builder(hostName, port, useSsl)
                            .withTimeout(timeoutMs)
                            .withMetadata(headers)
                            .build(),
                    cryptoEngine != null
                            ? cryptoEngine
                            : new TokenCryptoEngineFactory(new InMemoryKeyStore()),
                    devKey,
                    tokenCluster == null ? SANDBOX : tokenCluster,
                    browserFactory);
        }
    }

    private class MemberFunction implements Function<MemberAsync, Member> {
        public Member apply(MemberAsync memberAsync) {
            return memberAsync.sync();
        }
    }
}
