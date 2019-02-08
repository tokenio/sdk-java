/**
 * Copyright (c) 2019 Token, Inc.
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
import static io.grpc.Status.NOT_FOUND;
import static io.token.TokenClient.TokenCluster.SANDBOX;
import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.util.Util.normalizeAlias;
import static io.token.util.Util.toAddAliasOperation;
import static io.token.util.Util.toAddAliasOperationMetadata;
import static io.token.util.Util.toAddKeyOperation;
import static io.token.util.Util.toRecoveryAgentOperation;
import static java.util.Collections.singletonList;

import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.token.exceptions.VerificationException;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.bank.BankProtos.Bank;
import io.token.proto.common.member.MemberProtos;
import io.token.proto.common.member.MemberProtos.CreateMemberType;
import io.token.proto.common.member.MemberProtos.MemberOperation;
import io.token.proto.common.member.MemberProtos.MemberOperationMetadata;
import io.token.proto.common.member.MemberProtos.MemberRecoveryOperation;
import io.token.proto.common.member.MemberProtos.MemberRecoveryOperation.Authorization;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos.TokenMember;
import io.token.rpc.Client;
import io.token.rpc.ClientFactory;
import io.token.rpc.SslConfig;
import io.token.rpc.UnauthenticatedClient;
import io.token.rpc.client.RpcChannelFactory;
import io.token.security.CryptoEngine;
import io.token.security.CryptoEngineFactory;
import io.token.security.InMemoryKeyStore;
import io.token.security.KeyStore;
import io.token.security.Signer;
import io.token.security.TokenCryptoEngine;
import io.token.security.TokenCryptoEngineFactory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

public class TokenClient implements Closeable {
    private static final long SHUTDOWN_DURATION_MS = 10000L;

    protected final ManagedChannel channel;
    protected final CryptoEngineFactory cryptoFactory;
    protected final TokenCluster tokenCluster;

    /**
     * Creates an instance of a Token SDK.
     *
     * @param channel GRPC channel
     * @param cryptoFactory crypto factory instance
     * @param tokenCluster token cluster
     */
    protected TokenClient(
            ManagedChannel channel,
            CryptoEngineFactory cryptoFactory,
            TokenCluster tokenCluster) {
        this.channel = channel;
        this.cryptoFactory = cryptoFactory;
        this.tokenCluster = tokenCluster;
    }

    @Override
    public void close() {
        channel.shutdown();
        try {
            channel.awaitTermination(SHUTDOWN_DURATION_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Creates a new {@link Builder} instance that is used to configure and
     * build a {@link TokenClient} instance.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new instance of {@link TokenClient} that's configured to use
     * the specified environment.
     *
     * @param cluster token cluster to connect to
     * @param developerKey developer key
     * @return {@link TokenClient} instance
     */
    public static TokenClient create(TokenCluster cluster, String developerKey) {
        return TokenClient.builder()
                .connectTo(cluster)
                .devKey(developerKey)
                .build();
    }

    /**
     * Resolve an alias to a TokenMember object, containing member ID and the alias with
     * the correct type.
     *
     * @param alias alias to resolve
     * @return TokenMember
     */
    public Observable<TokenMember> resolveAlias(Alias alias) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.resolveAlias(alias);
    }

    /**
     * Resolve an alias to a TokenMember object, containing member ID and the alias with
     * the correct type.
     *
     * @param alias alias to resolve
     * @return TokenMember
     */
    public TokenMember resolveAliasBlocking(Alias alias) {
        return resolveAlias(alias).blockingSingle();
    }

    /**
     * Looks up member id for a given alias.
     *
     * @param alias alias to check
     * @return member id, or throws exception if member not found
     */
    public Observable<String> getMemberId(Alias alias) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.getMemberId(alias);
    }

    /**
     * Looks up member id for a given alias.
     *
     * @param alias alias to check
     * @return member id, or throws exception if member not found
     */
    public String getMemberIdBlocking(Alias alias) {
        return getMemberId(alias).blockingSingle();
    }

    /**
     * Creates a new Token member with a set of auto-generated keys, an alias, and member type.
     *
     * @param alias nullable member alias to use, must be unique. If null, then no alias will
     *     be created with the member.
     * @param memberType the type of member to register
     * @return newly created member
     */
    protected Observable<Member> createMemberImpl(
            final Alias alias,
            final CreateMemberType memberType) {
        final UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated
                .createMemberId(memberType, null)
                .flatMap(new Function<String, Observable<Member>>() {
                    public Observable<Member> apply(String memberId) {
                        return setUpMemberImpl(alias, memberId, generateKeys(memberId));
                    }
                });
    }

    /**
     * Sets up a member given a specific ID of a member that already exists in the system. If
     * the member ID already has keys, this will not succeed. Used for testing since this
     * gives more control over the member creation process.
     *
     * <p>Adds an alias and a set of auto-generated keys to the member.</p>
     *
     * @param alias nullable member alias to use, must be unique. If null, then no alias will
     *     be created with the member
     * @param memberId member id
     * @return newly created member
     */
    protected Observable<Member> setUpMemberImpl(
            final Alias alias,
            final String memberId,
            final List<Key> keys) {
        final UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        final CryptoEngine crypto = cryptoFactory.create(memberId);
        return unauthenticated.getDefaultAgent()
                .flatMap(new Function<String, Observable<MemberProtos.Member>>() {
                    public Observable<MemberProtos.Member> apply(String agentId) {
                        List<MemberOperation> operations = new ArrayList<>();
                        for (Key key : keys) {
                            operations.add(toAddKeyOperation(key));
                        }
                        operations.add(toRecoveryAgentOperation(agentId));

                        if (alias != null) {
                            operations.add(toAddAliasOperation(
                                    normalizeAlias(alias)));
                        }
                        List<MemberOperationMetadata> metadata = alias == null
                                ? Collections.<MemberOperationMetadata>emptyList()
                                : singletonList(toAddAliasOperationMetadata(
                                        normalizeAlias(alias)));
                        Signer signer = crypto.createSigner(PRIVILEGED);
                        return unauthenticated.createMember(
                                memberId,
                                operations,
                                metadata,
                                signer);
                    }
                })
                .flatMap(new Function<MemberProtos.Member, Observable<Member>>() {
                    public Observable<Member> apply(MemberProtos.Member member) {
                        Client client = ClientFactory.authenticated(
                                channel,
                                member.getId(),
                                crypto);
                        return Observable.just(new Member(
                                member,
                                client,
                                tokenCluster));
                    }
                });
    }

    protected List<Key> generateKeys(String memberId) {
        CryptoEngine crypto = cryptoFactory.create(memberId);
        List<Key> keys = new ArrayList<>();
        keys.add(crypto.generateKey(PRIVILEGED));
        keys.add(crypto.generateKey(STANDARD));
        keys.add(crypto.generateKey(LOW));
        return keys;
    }

    /**
     * Return a Member set up to use some Token member's keys (assuming we have them).
     *
     * @param memberId member id
     * @return member
     */
    protected Observable<Member> getMemberImpl(String memberId) {
        CryptoEngine crypto = cryptoFactory.create(memberId);
        final Client client = ClientFactory.authenticated(channel, memberId, crypto);
        return client
                .getMember(memberId)
                .map(new Function<MemberProtos.Member, Member>() {
                    public Member apply(MemberProtos.Member member) {
                        return new Member(member, client, tokenCluster);
                    }
                });
    }

    /**
     * Completes account recovery.
     *
     * @param memberId the member id
     * @param recoveryOperations the member recovery operations
     * @param privilegedKey the privileged public key in the member recovery operations
     * @param cryptoEngine the new crypto engine
     * @return an observable of the updated member
     */
    public Observable<Member> completeRecoveryImpl(
            String memberId,
            List<MemberRecoveryOperation> recoveryOperations,
            Key privilegedKey,
            final CryptoEngine cryptoEngine) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated
                .completeRecovery(memberId, recoveryOperations, privilegedKey, cryptoEngine)
                .map(new Function<MemberProtos.Member, Member>() {
                    public Member apply(MemberProtos.Member member) {
                        Client client = ClientFactory.authenticated(
                                channel,
                                member.getId(),
                                cryptoEngine);
                        return new Member(member, client, tokenCluster);
                    }
                });
    }

    /**
     * Completes account recovery if the default recovery rule was set.
     *
     * @param memberId the member id
     * @param verificationId the verification id
     * @param code the code
     * @return the new member
     */
    public Observable<Member> completeRecoveryWithDefaultRuleImpl(
            String memberId,
            String verificationId,
            String code) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        final CryptoEngine cryptoEngine = new TokenCryptoEngine(memberId, new InMemoryKeyStore());
        return unauthenticated
                .completeRecoveryWithDefaultRule(memberId, verificationId, code, cryptoEngine)
                .map(new Function<MemberProtos.Member, Member>() {
                    public Member apply(MemberProtos.Member member) {
                        Client client = ClientFactory.authenticated(
                                channel,
                                member.getId(),
                                cryptoEngine);
                        return new Member(member, client, tokenCluster);
                    }
                });
    }

    /**
     * Begins account recovery.
     *
     * @param alias the alias used to recover
     * @return the verification id
     */
    public Observable<String> beginRecovery(Alias alias) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.beginRecovery(alias);
    }

    /**
     * Begins account recovery.
     *
     * @param alias the alias used to recover
     * @return the verification id
     */
    public String beginRecoveryBlocking(Alias alias) {
        return beginRecovery(alias).blockingSingle();
    }

    /**
     * Create a recovery authorization for some agent to sign.
     *
     * @param memberId Id of member we claim to be.
     * @param privilegedKey new privileged key we want to use.
     * @return authorization structure for agent to sign
     */
    public Observable<Authorization> createRecoveryAuthorization(
            String memberId,
            Key privilegedKey) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.createRecoveryAuthorization(memberId, privilegedKey);
    }


    /**
     * Create a recovery authorization for some agent to sign.
     *
     * @param memberId Id of member we claim to be.
     * @param privilegedKey new privileged key we want to use.
     * @return authorization structure for agent to sign
     */
    public Authorization createRecoveryAuthorizationBlocking(
            String memberId,
            Key privilegedKey) {
        return createRecoveryAuthorization(memberId, privilegedKey).blockingSingle();
    }

    /**
     * Gets recovery authorization from Token.
     *
     * @param verificationId the verification id
     * @param code the code
     * @param key the privileged key
     * @return the member recovery operation
     * @throws VerificationException if the code verification fails
     */
    public Observable<MemberRecoveryOperation> getRecoveryAuthorization(
            String verificationId,
            String code,
            Key key) throws VerificationException {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.getRecoveryAuthorization(verificationId, code, key);
    }


    /**
     * Gets recovery authorization from Token.
     *
     * @param verificationId the verification id
     * @param code the code
     * @param key the privileged key
     * @return the member recovery operation
     * @throws VerificationException if the code verification fails
     */
    public MemberRecoveryOperation getRecoveryAuthorizationBlocking(
            String verificationId,
            String code,
            Key key) throws VerificationException {
        return getRecoveryAuthorization(verificationId, code, key).blockingSingle();
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
    public Observable<List<Bank>> getBanks(
            @Nullable List<String> bankIds,
            @Nullable Integer page,
            @Nullable Integer perPage)  {
        return getBanks(bankIds, null, null, page, perPage, null, null);
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
     * @param provider If specified, return banks whose 'provider' matches the given provider
     *     (case insensitive).
     * @return a list of banks
     */
    public Observable<List<Bank>> getBanks(
            @Nullable String search,
            @Nullable String country,
            @Nullable Integer page,
            @Nullable Integer perPage,
            @Nullable String sort,
            @Nullable String provider) {
        return getBanks(null, search, country, page, perPage, sort, provider);
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
     * @param provider If specified, return banks whose 'provider' matches the given provider
     *     (case insensitive).
     * @return a list of banks
     */
    public Observable<List<Bank>> getBanks(
            @Nullable List<String> bankIds,
            @Nullable String search,
            @Nullable String country,
            @Nullable Integer page,
            @Nullable Integer perPage,
            @Nullable String sort,
            @Nullable String provider) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.getBanks(bankIds, search, country, page, perPage, sort, provider);
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
    public List<Bank> getBanksBlocking(
            @Nullable List<String> bankIds,
            @Nullable Integer page,
            @Nullable Integer perPage)  {
        return getBanks(bankIds, page, perPage).blockingSingle();
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
     * @param provider If specified, return banks whose 'provider' matches the given provider
     *     (case insensitive).
     * @return a list of banks
     */
    public List<Bank> getBanksBlocking(
            @Nullable String search,
            @Nullable String country,
            @Nullable Integer page,
            @Nullable Integer perPage,
            @Nullable String sort,
            @Nullable String provider) {
        return getBanks(search, country, page, perPage, sort, provider)
                .blockingSingle();
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
     * @param provider If specified, return banks whose 'provider' matches the given provider
     *     (case insensitive).
     * @return a list of banks
     */
    public List<Bank> getBanksBlocking(
            @Nullable List<String> bankIds,
            @Nullable String search,
            @Nullable String country,
            @Nullable Integer page,
            @Nullable Integer perPage,
            @Nullable String sort,
            @Nullable String provider) {
        return getBanks(bankIds, search, country, page, perPage, sort, provider)
                .blockingSingle();
    }

    /**
     * Returns a list of countries with Token-enabled banks.
     *
     * @param provider If specified, return banks whose 'provider' matches the given provider
     *     (case insensitive).
     * @return a list of country codes
     */
    public Observable<List<String>> getCountries(String provider) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.getCountries(provider);
    }

    /**
     * Returns a list of countries with Token-enabled banks.
     *
     * @param provider If specified, return banks whose 'provider' matches the given provider
     *     (case insensitive).
     * @return a list of country codes
     */
    public List<String> getCountriesBlocking(String provider) {
        return getCountries(provider).blockingSingle();
    }

    /**
     * Defines Token cluster to connect to.
     */
    public enum TokenCluster {
        PRODUCTION("api-grpc.token.io"),
        INTEGRATION("api-grpc.int.token.io"),
        SANDBOX("api-grpc.sandbox.token.io"),
        STAGING("api-grpc.stg.token.io"),
        PERFORMANCE("api-grpc.perf.token.io"),
        DEVELOPMENT("api-grpc.dev.token.io");

        private final String url;

        TokenCluster(String url) {
            this.url = url;
        }

        public String url() {
            return url;
        }
    }

    /**
     * Used to create a new {@link TokenClient} instances.
     */
    public static class Builder<T extends Builder<T>> {
        private static final long DEFAULT_TIMEOUT_MS = 10_000L;
        private static final int DEFAULT_SSL_PORT = 443;

        protected int port;
        protected boolean useSsl;
        protected TokenCluster tokenCluster;
        protected String hostName;
        protected long timeoutMs;
        protected CryptoEngineFactory cryptoEngine;
        protected String devKey;
        protected SslConfig sslConfig;

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
        public T hostName(String hostName) {
            this.hostName = hostName;
            return (T) this;
        }

        /**
         * Sets the port of the Token Gateway Service to connect to.
         *
         * @param port port number
         * @return this builder instance
         */
        public T port(int port) {
            this.port = port;
            this.useSsl = port == DEFAULT_SSL_PORT;
            return (T) this;
        }

        /**
         * Sets Token cluster to connect to.
         *
         * @param cluster {@link TokenCluster} instance.
         * @return this builder instance
         */
        public T connectTo(TokenCluster cluster) {
            this.tokenCluster = cluster;
            this.hostName = cluster.url();
            return (T) this;
        }

        /**
         * Sets timeoutMs that is used for the RPC calls.
         *
         * @param timeoutMs RPC call timeoutMs
         * @return this builder instance
         */
        public T timeout(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return (T) this;
        }

        /**
         * Sets the keystore to be used with the SDK.
         *
         * @param keyStore the keystore to be used
         * @return this builder instance
         */
        public T withKeyStore(KeyStore keyStore) {
            this.cryptoEngine = new TokenCryptoEngineFactory(keyStore);
            return (T) this;
        }

        /**
         * Sets the crypto engine to be used with the SDK.
         *
         * @param cryptoEngineFactory the crypto engine factory to use
         * @return this builder instance
         */
        public T withCryptoEngine(CryptoEngineFactory cryptoEngineFactory) {
            this.cryptoEngine = cryptoEngineFactory;
            return (T) this;
        }

        /**
         * Sets configuration parameters for tls client. Can be used to specify specific
         * trusted certificates.
         *
         * @param sslConfig tls configuration to use
         * @return this builder instance
         */
        public T withSslConfig(SslConfig sslConfig) {
            this.sslConfig = sslConfig;
            return (T) this;
        }

        /**
         * Sets the developer key to be used with the SDK.
         *
         * @param devKey developer key
         * @return this builder instance
         */
        public T devKey(String devKey) {
            this.devKey = devKey;
            return (T) this;
        }

        /**
         * Builds and returns a new {@link TokenClient} instance.
         *
         * @return {@link TokenClient} instance
         */
        public TokenClient build() {
            Metadata headers = getHeaders();
            return new TokenClient(
                    RpcChannelFactory
                            .builder(hostName, port, useSsl)
                            .withTimeout(timeoutMs)
                            .withMetadata(headers)
                            .withClientSsl(sslConfig)
                            .build(),
                    cryptoEngine != null
                            ? cryptoEngine
                            : new TokenCryptoEngineFactory(new InMemoryKeyStore()),
                    tokenCluster == null ? SANDBOX : tokenCluster);
        }

        protected Metadata getHeaders() {
            if (devKey == null || devKey.isEmpty()) {
                throw new StatusRuntimeException(Status.INVALID_ARGUMENT
                        .withDescription("Please provide a developer key."
                                + " Contact Token for more details."));
            }
            Metadata headers = new Metadata();
            headers.put(
                    Metadata.Key.of("token-dev-key", ASCII_STRING_MARSHALLER),
                    devKey);

            ClassLoader classLoader = io.token.TokenClient.class.getClassLoader();
            try {
                Class<?> projectClass = classLoader.loadClass("io.token.gradle.TokenProject");

                String version = (String) projectClass
                        .getMethod("getVersion")
                        .invoke(null);
                String platform = (String) projectClass
                        .getMethod("getPlatform")
                        .invoke(null);
                headers.put(
                        Metadata.Key.of("token-sdk", ASCII_STRING_MARSHALLER),
                        platform);
                headers.put(
                        Metadata.Key.of("token-sdk-version", ASCII_STRING_MARSHALLER),
                        version);
            } catch (Exception e) {
                throw new StatusRuntimeException(NOT_FOUND
                        .withDescription("Plugin io.token.gradle.TokenProject is not found"
                                + " in this module"));
            }
            return headers;
        }
    }
}
