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
import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.util.Util.generateNonce;
import static java.util.Arrays.asList;

import com.google.common.annotations.VisibleForTesting;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.token.DeviceInfo;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.blob.BlobProtos;
import io.token.proto.common.member.MemberProtos.MemberRecoveryOperation;
import io.token.proto.common.member.MemberProtos.ReceiptContact;
import io.token.proto.common.notification.NotificationProtos.AddKey;
import io.token.proto.common.notification.NotificationProtos.DeviceMetadata;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.security.SecurityProtos;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.proto.common.token.TokenProtos.TokenRequestOptions;
import io.token.rpc.client.RpcChannelFactory;
import io.token.security.CryptoEngine;
import io.token.security.CryptoEngineFactory;
import io.token.security.InMemoryKeyStore;
import io.token.security.TokenCryptoEngineFactory;
import io.token.tokenrequest.TokenRequest;
import io.token.tokenrequest.TokenRequestResult;
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
     * @param browserFactory browser factory
     */
    protected TokenClient(
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
     * @return {@link TokenClient} instance
     */
    public static TokenClient create(TokenCluster cluster) {
        return TokenClient.builder()
                .connectTo(cluster)
                .build();
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
     * @return newly created member
     */
    public Observable<Member> createMember(final Alias alias) {
        return createMember(alias, null, null);
    }

    /**
     * Creates a new Token member with a set of auto-generated keys, an alias, and member type.
     *
     * @param alias nullable member alias to use, must be unique. If null, then no alias will
     *     be created with the member.
     * @param recoveryAgent member id of the primary recovery agent.
     * @return newly created member
     */
    public Observable<Member> createMember(final Alias alias, String recoveryAgent) {
        return createMember(alias, recoveryAgent, null);
    }

    /**
     * Creates a new Token member with a set of auto-generated keys, an alias, member type
     * and recovery agent.
     *
     * @param alias nullable member alias to use, must be unique. If null, then no alias will
     *     be created with the member.
     * @param recoveryAgent member id of the primary recovery agent.
     * @param realmId member id of an existing Member to whose realm this new member belongs.
     * @return newly created member
     */
    public Observable<Member> createMember(final Alias alias,
                                           @Nullable final String recoveryAgent,
                                           @Nullable final String realmId) {
        return createMemberImpl(alias, PERSONAL, null, recoveryAgent, realmId)
                .map(new Function<io.token.Member, Member>() {
                    @Override
                    public Member apply(io.token.Member mem) {
                        CryptoEngine crypto = cryptoFactory.create(mem.memberId());
                        final Client client = ClientFactory.authenticated(
                                channel,
                                mem.memberId(),
                                crypto);
                        return new Member(
                                mem.memberId(),
                                mem.partnerId(),
                                client,
                                mem.getTokenCluster(),
                                browserFactory);
                    }
                });
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
     * Creates a new personal-use Token member for the Banks with a set of
     * auto-generated keys, an alias and recovery agent set as the Bank.
     *
     * @param alias alias to associate with member
     * @param recoveryAgent member id of the primary recovery agent.
     * @return newly created member
     */
    public Member createMemberBlocking(Alias alias, String recoveryAgent) {
        return createMember(alias, recoveryAgent).blockingSingle();
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
        return setUpMemberImpl(alias, memberId, null)
                .map(new Function<io.token.Member, Member>() {
                    @Override
                    public Member apply(io.token.Member mem) {
                        return new Member(
                                mem.memberId(),
                                mem.partnerId(),
                                client,
                                mem.getTokenCluster(),
                                browserFactory);
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
        return getMemberImpl(memberId, client)
                .map(new Function<io.token.Member, Member>() {
                    @Override
                    public Member apply(io.token.Member mem) {
                        return new Member(
                                mem.memberId(),
                                mem.partnerId(),
                                client,
                                mem.getTokenCluster(),
                                browserFactory);
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
                        return new Member(
                                mem.memberId(),
                                mem.partnerId(),
                                client,
                                mem.getTokenCluster(),
                                browserFactory);
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
     * @param cryptoEngine the new crypto engine
     * @return the new member
     */
    public Observable<Member> completeRecoveryWithDefaultRule(
            String memberId,
            String verificationId,
            String code,
            final CryptoEngine cryptoEngine) {
        return completeRecoveryWithDefaultRuleImpl(memberId, verificationId, code, cryptoEngine)
                .map(new Function<io.token.Member, Member>() {
                    @Override
                    public Member apply(io.token.Member mem) {
                        final Client client = ClientFactory.authenticated(
                                channel,
                                mem.memberId(),
                                cryptoEngine);
                        return new Member(
                                mem.memberId(),
                                mem.partnerId(),
                                client,
                                mem.getTokenCluster(),
                                browserFactory);
                    }
                });
    }

    /**
     * Completes account recovery if the default recovery rule was set.
     *
     * @param memberId the member id
     * @param verificationId the verification id
     * @param code the code
     * @param cryptoEngine the new crypto engine
     * @return the new member
     */
    public Member completeRecoveryWithDefaultRuleBlocking(
            String memberId,
            String verificationId,
            String code,
            final CryptoEngine cryptoEngine) {
        return completeRecoveryWithDefaultRule(memberId, verificationId, code, cryptoEngine)
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
    public Observable<DeviceInfo> provisionDevice(Alias alias) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated
                .getMemberId(alias)
                .map(new Function<String, DeviceInfo>() {
                    public DeviceInfo apply(String memberId) {
                        CryptoEngine crypto = cryptoFactory.create(memberId);
                        return new DeviceInfo(
                                memberId,
                                asList(
                                        crypto.generateKey(PRIVILEGED),
                                        crypto.generateKey(STANDARD),
                                        crypto.generateKey(LOW)));
                    }
                });
    }

    /**
     * Provisions a new device for an existing user. The call generates a set
     * of keys that are returned back. The keys need to be approved by an
     * existing device/keys.
     *
     * @param alias member id to provision the device for
     * @return device information
     */
    public DeviceInfo provisionDeviceBlocking(Alias alias) {
        return provisionDevice(alias).blockingSingle();
    }

    /**
     * Notifies to add a key.
     *
     * @param alias alias to notify
     * @param keys keys that need approval
     * @param deviceMetadata device metadata of the keys
     * @return status of the notification
     */
    public Observable<NotifyStatus> notifyAddKey(
            Alias alias,
            List<SecurityProtos.Key> keys,
            DeviceMetadata deviceMetadata) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        AddKey addKey = AddKey.newBuilder()
                .addAllKeys(keys)
                .setDeviceMetadata(deviceMetadata)
                .build();
        return unauthenticated.notifyAddKey(alias, addKey);
    }

    /**
     * Notifies to add a key.
     *
     * @param alias alias to notify
     * @param keys keys that need approval
     * @param deviceMetadata device metadata of the keys
     * @return status of the notification
     */
    public NotifyStatus notifyAddKeyBlocking(
            Alias alias,
            List<SecurityProtos.Key> keys,
            DeviceMetadata deviceMetadata) {
        return notifyAddKey(alias, keys, deviceMetadata).blockingSingle();
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
     * Invalidate a notification.
     *
     * @param notificationId notification id to invalidate
     * @return status of the invalidation request
     */
    public Observable<NotifyStatus> invalidateNotification(String notificationId) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.invalidateNotification(notificationId);
    }

    /**
     * Invalidate a notification.
     *
     * @param notificationId notification id to invalidate
     * @return status of the invalidation request
     */
    public NotifyStatus invalidateNotificationBlocking(String notificationId) {
        return invalidateNotification(notificationId).blockingSingle();
    }

    /**
     * Retrieves a blob from the server.
     *
     * @param blobId id of the blob
     * @return Blob
     */
    public Observable<BlobProtos.Blob> getBlob(String blobId) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.getBlob(blobId);
    }

    /**
     * Retrieves a blob from the server.
     *
     * @param blobId id of the blob
     * @return Blob
     */
    public BlobProtos.Blob getBlobBlocking(String blobId) {
        return getBlob(blobId).blockingSingle();
    }

    /**
     * Get the token request result based on a token's tokenRequestId.
     *
     * @param tokenRequestId token request id
     * @return token request result
     */
    public Observable<TokenRequestResult> getTokenRequestResult(String tokenRequestId) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.getTokenRequestResult(tokenRequestId);
    }

    /**
     * Get the token request result based on a token's tokenRequestId.
     *
     * @param tokenRequestId token request id
     * @return token request result
     */
    public TokenRequestResult getTokenRequestResultBlocking(String tokenRequestId) {
        return getTokenRequestResult(tokenRequestId).blockingSingle();
    }

    /**
     * Return a TokenRequest that was previously stored.
     *
     * @param requestId request id
     * @return token request that was stored with the request id
     */
    public Observable<TokenRequest> retrieveTokenRequest(String requestId) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.retrieveTokenRequest(requestId);
    }

    /**
     * Return a TokenRequest that was previously stored.
     *
     * @param requestId request id
     * @return token request that was stored with the request id
     */
    public TokenRequest retrieveTokenRequestBlocking(String requestId) {
        return retrieveTokenRequest(requestId).blockingSingle();
    }

    /**
     * Updates an existing token request.
     *
     * @param requestId token request ID
     * @param options new token request options
     * @return completable
     */
    public Completable updateTokenRequest(String requestId, TokenRequestOptions options) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.updateTokenRequest(requestId, options);
    }

    /**
     * Updates an existing token request.
     *
     * @param requestId token request ID
     * @param options new token request options
     */
    public void updateTokenRequestBlocking(String requestId, TokenRequestOptions options) {
        updateTokenRequest(requestId, options).blockingAwait();
    }

    public static class Builder extends io.token.TokenClient.Builder<Builder> {
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

        @Override
        protected String getPlatform() {
            return "java-user";
        }
    }
}
