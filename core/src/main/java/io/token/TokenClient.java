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
import static io.grpc.Status.UNIMPLEMENTED;
import static io.token.TokenClient.TokenCluster.SANDBOX;
import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.util.Util.toAddAliasOperation;
import static io.token.util.Util.toAddAliasOperationMetadata;
import static io.token.util.Util.toAddKeyOperation;
import static io.token.util.Util.toRecoveryAgentOperation;
import static java.util.Collections.singletonList;

import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.token.exceptions.VerificationException;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.bank.BankProtos.Bank;
import io.token.proto.common.bank.BankProtos.BankFilter.BankFeatures;
import io.token.proto.common.member.MemberProtos;
import io.token.proto.common.member.MemberProtos.CreateMemberType;
import io.token.proto.common.member.MemberProtos.MemberOperation;
import io.token.proto.common.member.MemberProtos.MemberOperationMetadata;
import io.token.proto.common.member.MemberProtos.MemberRecoveryOperation;
import io.token.proto.common.member.MemberProtos.MemberRecoveryOperation.Authorization;
import io.token.proto.common.security.SecurityProtos;
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
import io.token.security.TokenCryptoEngineFactory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
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
     * <p>Impl method returns incomplete member object that can be used for its instance
     * fields but will not be able to make calls.
     *
     * @param alias nullable member alias to use, must be unique. If null, then no alias will
     *     be created with the member.
     * @param memberType the type of member to register
     * @param partnerId ID of the partner member
     * @param recoveryAgent member id of the primary recovery agent
     * @param realmId member id of an existing Member to whom the new member belongs.
     * @return newly created member
     */
    protected Observable<Member> createMemberImpl(
            final Alias alias,
            final CreateMemberType memberType,
            @Nullable String partnerId,
            @Nullable final String recoveryAgent,
            @Nullable final String realmId) {
        final UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated
                .createMemberId(memberType, null, partnerId, realmId)
                .flatMap(new Function<String, Observable<Member>>() {
                    public Observable<Member> apply(String memberId) {
                        return setUpMemberImpl(alias, memberId, recoveryAgent);
                    }
                });
    }

    /**
     * Sets up a member given a specific ID of a member that already exists in the system. If
     * the member ID already has keys, this will not succeed. Used for testing since this
     * gives more control over the member creation process.
     *
     * <p>Impl method returns incomplete member object that can be used for its instance
     * fields but will not be able to make calls.
     *
     * <p>Adds an alias and a set of auto-generated keys to the member.</p>
     *
     * @param alias nullable member alias to use, must be unique. If null, then no alias will
     *     be created with the member
     * @param memberId member id
     * @param agent member id of the primary recovery agent.
     * @return newly created member
     */
    protected Observable<Member> setUpMemberImpl(final Alias alias,
                                                 final String memberId,
                                                 final String agent) {
        final UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        // TODO(RD-3727): we probably should not set recovery agent for realmed members at all
        return (agent == null ? unauthenticated.getDefaultAgent() : Observable.just(agent))
                .flatMap(new Function<String, Observable<MemberProtos.Member>>() {
                    public Observable<MemberProtos.Member> apply(String agentId) {
                        CryptoEngine crypto = cryptoFactory.create(memberId);
                        List<MemberOperation> operations = new ArrayList<>();
                        operations.add(
                                toAddKeyOperation(crypto.generateKey(PRIVILEGED)));
                        operations.add(
                                toAddKeyOperation(crypto.generateKey(STANDARD)));
                        operations.add(
                                toAddKeyOperation(crypto.generateKey(LOW)));
                        operations.add(toRecoveryAgentOperation(agentId));

                        if (alias != null) {
                            operations.add(toAddAliasOperation(alias));
                        }
                        List<MemberOperationMetadata> metadata = alias == null
                                ? Collections.<MemberOperationMetadata>emptyList()
                                : singletonList(toAddAliasOperationMetadata(alias));
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
                        return Observable.just(new Member(
                                member.getId(),
                                member.getPartnerId(),
                                member.getRealmId(),
                                null,
                                tokenCluster));
                    }
                });
    }

    /**
     * Return a Member set up to use some Token member's keys (assuming we have them).
     *
     * <p>Impl method returns incomplete member object that can be used for its instance
     * fields but will not be able to make calls.
     *
     * @param memberId member id
     * @param client client
     * @return member
     */
    protected Observable<Member> getMemberImpl(final String memberId, final Client client) {
        return client
                .getMember(memberId)
                .map(new Function<MemberProtos.Member, Member>() {
                    public Member apply(MemberProtos.Member member) {
                        return new Member(
                                member.getId(),
                                member.getPartnerId(),
                                member.getRealmId(),
                                null,
                                tokenCluster);
                    }
                });
    }

    /**
     * Completes account recovery.
     *
     * <p>Impl method returns incomplete member object that can be used for its instance
     * fields but will not be able to make calls.
     *
     * @param memberId the member id
     * @param recoveryOperations the member recovery operations
     * @param privilegedKey the privileged public key in the member recovery operations
     * @param cryptoEngine the new crypto engine
     * @return an observable of the updated member
     */
    public Observable<Member> completeRecoveryImpl(
            final String memberId,
            List<MemberRecoveryOperation> recoveryOperations,
            SecurityProtos.Key privilegedKey,
            final CryptoEngine cryptoEngine) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated
                .completeRecovery(memberId, recoveryOperations, privilegedKey, cryptoEngine)
                .map(new Function<MemberProtos.Member, Member>() {
                    public Member apply(MemberProtos.Member member) {
                        return new Member(
                                member.getId(),
                                member.getPartnerId(),
                                member.getRealmId(),
                                null,
                                tokenCluster);
                    }
                });
    }

    /**
     * Completes account recovery if the default recovery rule was set.
     *
     * <p>Impl method returns incomplete member object that can be used for its instance
     * fields but will not be able to make calls.
     *
     * @param memberId the member id
     * @param verificationId the verification id
     * @param code the code
     * @param cryptoEngine crypto engine
     * @return the new member
     */
    public Observable<Member> completeRecoveryWithDefaultRuleImpl(
            String memberId,
            String verificationId,
            String code,
            final CryptoEngine cryptoEngine) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated
                .completeRecoveryWithDefaultRule(memberId, verificationId, code, cryptoEngine)
                .map(new Function<MemberProtos.Member, Member>() {
                    public Member apply(MemberProtos.Member member) {
                        return new Member(
                                member.getId(),
                                member.getPartnerId(),
                                member.getRealmId(),
                                null,
                                tokenCluster);
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
        return getBanks(bankIds, search, country, page, perPage, sort, provider, null);
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
     * @param bankFeatures If specified, return banks who meet the bank features requirement.
     * @return a list of banks
     */
    public Observable<List<Bank>> getBanks(
            @Nullable List<String> bankIds,
            @Nullable String search,
            @Nullable String country,
            @Nullable Integer page,
            @Nullable Integer perPage,
            @Nullable String sort,
            @Nullable String provider,
            @Nullable BankFeatures bankFeatures) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.getBanks(
                bankIds,
                search,
                country,
                page,
                perPage,
                sort,
                provider,
                bankFeatures);
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
        private static final String DEFAULT_DEV_KEY = "4qY7lqQw8NOl9gng0ZHgT4xdiDqxqoGVutuZwrUYQsI";
        private static final long DEFAULT_TIMEOUT_MS = 10_000L;
        private static final int DEFAULT_SSL_PORT = 443;
        private static final String FEATURE_CODE_KEY = "feature-codes";

        protected int port;
        protected boolean useSsl;
        protected TokenCluster tokenCluster;
        protected String hostName;
        protected long timeoutMs;
        protected CryptoEngineFactory cryptoEngine;
        protected String devKey;
        protected SslConfig sslConfig;
        protected List<String> featureCodes;

        /**
         * Creates new builder instance with the defaults initialized.
         */
        public Builder() {
            this.devKey = DEFAULT_DEV_KEY;
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
         * Sets the feature codes to be used with the client.
         *
         * @param featureCodes feature codes
         * @return this builder instance
         */
        public T withFeatureCodes(String... featureCodes) {
            this.featureCodes = Arrays.asList(featureCodes);
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
            Metadata headers = new Metadata();
            headers.put(
                    Metadata.Key.of("token-dev-key", ASCII_STRING_MARSHALLER),
                    devKey);

            headers.put(
                    Metadata.Key.of("token-sdk", ASCII_STRING_MARSHALLER),
                    getPlatform());
            try {
                ClassLoader classLoader = io.token.TokenClient.class.getClassLoader();
                Class<?> projectClass = classLoader.loadClass("io.token.gradle.TokenProject");
                String version = (String) projectClass
                        .getMethod("getVersion")
                        .invoke(null);
                headers.put(
                        Metadata.Key.of("token-sdk-version", ASCII_STRING_MARSHALLER),
                        version);
            } catch (Exception e) {
                throw new StatusRuntimeException(NOT_FOUND
                        .withDescription("Plugin io.token.gradle.TokenProject is not found"
                                + " in this module"));
            }

            if (featureCodes != null) {
                for (String featureCode : featureCodes) {
                    headers.put(
                            Metadata.Key.of(FEATURE_CODE_KEY, ASCII_STRING_MARSHALLER),
                            featureCode);
                }
            }
            return headers;
        }

        protected String getPlatform() {
            throw UNIMPLEMENTED.asRuntimeException();
        }
    }
}
