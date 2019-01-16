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

import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.util.Util.normalizeAlias;
import static io.token.util.Util.toAddAliasOperation;
import static io.token.util.Util.toAddAliasOperationMetadata;
import static io.token.util.Util.toAddKeyOperation;
import static io.token.util.Util.toRecoveryAgentOperation;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import com.google.common.annotations.VisibleForTesting;
import io.grpc.ManagedChannel;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.token.exceptions.VerificationException;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.bank.BankProtos.Bank;
import io.token.proto.common.blob.BlobProtos.Blob;
import io.token.proto.common.member.MemberProtos;
import io.token.proto.common.member.MemberProtos.MemberOperation;
import io.token.proto.common.member.MemberProtos.MemberOperationMetadata;
import io.token.proto.common.member.MemberProtos.MemberRecoveryOperation;
import io.token.proto.common.member.MemberProtos.MemberRecoveryOperation.Authorization;
import io.token.proto.common.notification.NotificationProtos.AddKey;
import io.token.proto.common.notification.NotificationProtos.DeviceMetadata;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.rpc.Client;
import io.token.rpc.ClientFactory;
import io.token.rpc.UnauthenticatedClient;
import io.token.security.CryptoEngine;
import io.token.security.CryptoEngineFactory;
import io.token.security.Signer;

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
     * Checks if a given alias already exists.
     *
     * @param alias alias to check
     * @return {@code true} if alias exists, {@code false} otherwise
     */
    public Observable<Boolean> aliasExists(Alias alias) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.aliasExists(alias);
    }

    /**
     * Checks if a given alias already exists.
     *
     * @param alias alias to check
     * @return {@code true} if alias exists, {@code false} otherwise
     */
    public boolean aliasExistsBlocking(Alias alias) {
        return aliasExists(alias).blockingSingle();
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
                        CryptoEngine crypto = cryptoFactory.create(member.getId());
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
            List<Key> keys,
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
            List<Key> keys,
            DeviceMetadata deviceMetadata) {
        return notifyAddKey(alias, keys, deviceMetadata).blockingSingle();
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
    public Observable<Blob> getBlob(String blobId) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.getBlob(blobId);
    }

    /**
     * Retrieves a blob from the server.
     *
     * @param blobId id of the blob
     * @return Blob
     */
    public Blob getBlobBlocking(String blobId) {
        return getBlob(blobId).blockingSingle();
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
}
