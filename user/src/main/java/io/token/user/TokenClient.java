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

import static io.token.TokenClient.TokenCluster.SANDBOX;
import static io.token.proto.common.member.MemberProtos.CreateMemberType.PERSONAL;
import static io.token.util.Util.generateNonce;

import com.google.common.annotations.VisibleForTesting;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.member.MemberProtos;
import io.token.proto.common.member.MemberProtos.CreateMemberType;
import io.token.proto.common.member.MemberProtos.MemberRecoveryOperation;
import io.token.proto.common.member.MemberProtos.ReceiptContact;
import io.token.proto.common.notification.NotificationProtos.AddKey;
import io.token.proto.common.notification.NotificationProtos.DeviceMetadata;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.security.SecurityProtos;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.rpc.client.RpcChannelFactory;
import io.token.security.CryptoEngine;
import io.token.security.CryptoEngineFactory;
import io.token.security.InMemoryKeyStore;
import io.token.security.TokenCryptoEngine;
import io.token.security.TokenCryptoEngineFactory;
import io.token.user.browser.BrowserFactory;
import io.token.user.rpc.Client;
import io.token.user.rpc.ClientFactory;
import io.token.user.rpc.UnauthenticatedClient;

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
    public Observable<Member> createMember(
            final Alias alias,
            final CreateMemberType memberType) {
        return createMemberImpl(alias, memberType)
                .map(new Function<io.token.Member, Member>() {
                    @Override
                    public Member apply(io.token.Member mem) {
                        CryptoEngine crypto = cryptoFactory.create(mem.memberId());
                        final Client client = ClientFactory.authenticated(
                                channel,
                                mem.memberId(),
                                crypto);
                        return new Member(mem, client, browserFactory);
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
    @VisibleForTesting
    public Observable<Member> setUpMember(final Alias alias, final String memberId) {
        CryptoEngine crypto = cryptoFactory.create(memberId);
        final Client client = ClientFactory.authenticated(channel, memberId, crypto);
        return setUpMemberImpl(alias, memberId)
                .map(new Function<io.token.Member, Member>() {
                    @Override
                    public Member apply(io.token.Member mem) {
                        return new Member(mem, client, browserFactory);
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
    public Observable<Member> completeRecovery(
            String memberId,
            List<MemberRecoveryOperation> recoveryOperations,
            SecurityProtos.Key privilegedKey,
            final CryptoEngine cryptoEngine) {
        return completeRecoveryImpl(memberId, recoveryOperations, privilegedKey, cryptoEngine)
                .map(new Function<io.token.Member, Member>() {
                    @Override
                    public Member apply(io.token.Member mem) {
                        final Client client = ClientFactory.authenticated(
                                channel,
                                mem.memberId(),
                                cryptoEngine);
                        return new Member(mem, client, browserFactory);
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
    public Observable<Member> completeRecoveryWithDefaultRule(
            String memberId,
            String verificationId,
            String code) {
        final CryptoEngine cryptoEngine = new TokenCryptoEngine(memberId, new InMemoryKeyStore());
        return completeRecoveryWithDefaultRuleImpl(memberId, verificationId, code)
                .map(new Function<io.token.Member, Member>() {
                    @Override
                    public Member apply(io.token.Member mem) {
                        final Client client = ClientFactory.authenticated(
                                channel,
                                mem.memberId(),
                                cryptoEngine);
                        return new Member(mem, client, browserFactory);
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

    public static final class Builder extends io.token.TokenClient.Builder<Builder> {
        private BrowserFactory browserFactory;

        /**
         * Creates new builder instance with the defaults initialized.
         */
        public Builder() {
            super();
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

        @Override
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
    }
}
