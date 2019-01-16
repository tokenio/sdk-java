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

package io.token.user;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static io.grpc.Status.NOT_FOUND;
import static io.token.TokenClient.TokenCluster.SANDBOX;
import static io.token.proto.common.member.MemberProtos.CreateMemberType.PERSONAL;
import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.util.Util.generateNonce;
import static io.token.util.Util.normalizeAlias;
import static io.token.util.Util.toAddAliasOperation;
import static io.token.util.Util.toAddAliasOperationMetadata;
import static io.token.util.Util.toAddKeyOperation;
import static io.token.util.Util.toRecoveryAgentOperation;
import static java.util.Collections.singletonList;

import com.google.common.annotations.VisibleForTesting;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.member.MemberProtos;
import io.token.proto.common.member.MemberProtos.CreateMemberType;
import io.token.proto.common.member.MemberProtos.MemberOperation;
import io.token.proto.common.member.MemberProtos.MemberRecoveryOperation;
import io.token.proto.common.member.MemberProtos.ReceiptContact;
import io.token.proto.common.notification.NotificationProtos.AddKey;
import io.token.proto.common.notification.NotificationProtos.DeviceMetadata;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.security.SecurityProtos;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.rpc.SslConfig;
import io.token.rpc.client.RpcChannelFactory;
import io.token.security.CryptoEngine;
import io.token.security.CryptoEngineFactory;
import io.token.security.InMemoryKeyStore;
import io.token.security.KeyStore;
import io.token.security.Signer;
import io.token.security.TokenCryptoEngine;
import io.token.security.TokenCryptoEngineFactory;
import io.token.user.browser.BrowserFactory;
import io.token.user.rpc.Client;
import io.token.user.rpc.ClientFactory;
import io.token.user.rpc.UnauthenticatedClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

public class TokenClient extends io.token.TokenClient {
    private final BrowserFactory browserFactory;

    /**
     * Creates an instance of a Token SDK.
     *
     * @param channel GRPC channel
     * @param cryptoFactory crypto factory instance
     * @param tokenCluster token cluster
     */
    TokenClient(
            ManagedChannel channel,
            CryptoEngineFactory cryptoFactory,
            TokenCluster tokenCluster,
            BrowserFactory browserFactory) {
        super(channel, cryptoFactory, tokenCluster);
        this.browserFactory = browserFactory;
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
     * Creates a new Token member with a set of auto-generated keys, an alias, and member type.
     *
     * @param alias nullable member alias to use, must be unique. If null, then no alias will
     *     be created with the member.
     * @param memberType the type of member to register
     * @return newly created member
     */
    @Override
    public Observable<Member> createMember(
            final Alias alias,
            final CreateMemberType memberType) {
        final UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated
                .createMemberId(memberType, null)
                .flatMap(new Function<String, Observable<Member>>() {
                    public Observable<Member> apply(String memberId) {
                        return setUpMember(alias, memberId);
                    }
                });
    }

    /**
     * Creates a new personal-use Token member with a set of auto-generated keys and and an alias.
     *
     * @param alias alias to associate with member
     * @return newly created member
     */
    public Observable<Member> createMember(Alias alias) {
        return createMember(alias, PERSONAL);
    }

    /**
     * Creates a new Token member with a set of auto-generated keys, an alias, and member type.
     *
     * @param alias nullable member alias to use, must be unique. If null, then no alias will
     *     be created with the member.
     * @param memberType the type of member to register
     * @return newly created member
     */
    @Override
    public Member createMemberBlocking(
            final Alias alias,
            final CreateMemberType memberType) {
        return createMember(alias, memberType).blockingSingle();
    }

    /**
     * Creates a new personal-use Token member with a set of auto-generated keys and and an alias.
     *
     * @param alias alias to associate with member
     * @return newly created member
     */
    public Member createMemberBlocking(Alias alias) {
        return createMember(alias).blockingSingle();
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
    @Override
    public Observable<Member> setUpMember(final Alias alias, final String memberId) {
        final UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.getDefaultAgent()
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
                            operations.add(toAddAliasOperation(
                                    normalizeAlias(alias)));
                        }
                        List<MemberProtos.MemberOperationMetadata> metadata = alias == null
                                ? Collections.<MemberProtos.MemberOperationMetadata>emptyList()
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
                        CryptoEngine crypto = cryptoFactory.create(member.getId());
                        Client client = ClientFactory.authenticated(
                                channel,
                                member.getId(),
                                crypto);
                        return Observable.just(new Member(
                                member,
                                client,
                                tokenCluster,
                                browserFactory));
                    }
                });
    }

    /**
     * Return a Member set up to use some Token member's keys (assuming we have them).
     *
     * @param memberId member id
     * @return member
     */
    public Observable<Member> getMember(String memberId) {
        CryptoEngine crypto = cryptoFactory.create(memberId);
        final Client client = ClientFactory.authenticated(channel, memberId, crypto);
        return client
                .getMember(memberId)
                .map(new Function<MemberProtos.Member, Member>() {
                    public Member apply(MemberProtos.Member member) {
                        return new Member(member, client, tokenCluster, browserFactory);
                    }
                });
    }

    /**
     * Return a Member set up to use some Token member's keys (assuming we have them).
     *
     * @param memberId member id
     * @return member
     */
    public Member getMemberBlocking(String memberId) {
        return getMember(memberId).blockingSingle();
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
    @Override
    public Observable<Member> completeRecovery(
            String memberId,
            List<MemberRecoveryOperation> recoveryOperations,
            SecurityProtos.Key privilegedKey,
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
                        return new Member(member, client, tokenCluster, browserFactory);
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
    @Override
    public Member completeRecoveryBlocking(
            String memberId,
            List<MemberRecoveryOperation> recoveryOperations,
            SecurityProtos.Key privilegedKey,
            final CryptoEngine cryptoEngine) {
        return completeRecovery(memberId, recoveryOperations, privilegedKey, cryptoEngine)
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
    @Override
    public Observable<Member> completeRecoveryWithDefaultRule(
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
                        return new Member(member, client, tokenCluster, browserFactory);
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
    @Override
    public Member completeRecoveryWithDefaultRuleBlocking(
            String memberId,
            String verificationId,
            String code) {
        return completeRecoveryWithDefaultRule(memberId, verificationId, code).blockingSingle();
    }

    /**
     * Sends a notification to request a payment.
     *
     * @param tokenPayload the payload of a token to be sent
     * @return status of the notification request
     */
    public Observable<NotifyStatus> notifyPaymentRequest(TokenPayload tokenPayload) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        if (tokenPayload.getRefId().isEmpty()) {
            tokenPayload = tokenPayload.toBuilder().setRefId(generateNonce()).build();
        }
        return unauthenticated.notifyPaymentRequest(tokenPayload);
    }

    /**
     * Sends a notification to request a payment.
     *
     * @param tokenPayload the payload of a token to be sent
     * @return status of the notification request
     */
    public NotifyStatus notifyPaymentRequestBlocking(TokenPayload tokenPayload) {
        return notifyPaymentRequest(tokenPayload).blockingSingle();
    }

    /**
     * Notifies subscribed devices that a token should be created and endorsed.
     *
     * @param tokenRequestId the token request ID to send
     * @param keys keys to be added
     * @param deviceMetadata device metadata of the keys
     * @param receiptContact optional receipt contact to send
     * @return notify result of the notification request
     */
    public Observable<NotifyResult> notifyCreateAndEndorseToken(
            String tokenRequestId,
            @Nullable List<SecurityProtos.Key> keys,
            @Nullable DeviceMetadata deviceMetadata,
            @Nullable ReceiptContact receiptContact) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.notifyCreateAndEndorseToken(
                tokenRequestId,
                AddKey.newBuilder()
                        .addAllKeys(keys)
                        .setDeviceMetadata(deviceMetadata)
                        .build(),
                receiptContact);
    }

    /**
     * Notifies subscribed devices that a token should be created and endorsed.
     *
     * @param tokenRequestId the token request ID to send
     * @param keys keys to be added
     * @param deviceMetadata device metadata of the keys
     * @param receiptContact optional receipt contact to send
     * @return notify result of the notification request
     */
    public NotifyResult notifyCreateAndEndorseTokenBlocking(
            String tokenRequestId,
            @Nullable List<SecurityProtos.Key> keys,
            @Nullable DeviceMetadata deviceMetadata,
            @Nullable ReceiptContact receiptContact) {
        return notifyCreateAndEndorseToken(tokenRequestId, keys, deviceMetadata, receiptContact)
                .blockingSingle();
    }

    /**
     * Used to create a new {@link TokenClient} instances.
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
        private SslConfig sslConfig;
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
         * Sets configuration parameters for tls client. Can be used to specify specific
         * trusted certificates.
         *
         * @param sslConfig tls configuration to use
         * @return this builder instance
         */
        public Builder withSslConfig(SslConfig sslConfig) {
            this.sslConfig = sslConfig;
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
                    tokenCluster == null ? SANDBOX : tokenCluster,
                    browserFactory);
        }

        private Metadata getHeaders() {
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
