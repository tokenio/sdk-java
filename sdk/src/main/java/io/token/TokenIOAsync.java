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

import static io.grpc.Status.NOT_FOUND;
import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.util.Util.toAddKeyOperation;
import static io.token.util.Util.toAddUsernameOperation;
import static java.util.Arrays.asList;

import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.proto.common.member.MemberProtos;
import io.token.proto.common.member.MemberProtos.MemberOperation;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.rpc.Client;
import io.token.rpc.ClientFactory;
import io.token.rpc.UnauthenticatedClient;
import io.token.security.CryptoEngine;
import io.token.security.CryptoEngineFactory;
import io.token.security.Signer;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Func1;

/**
 * Create a new member using the {@link #createMember}  method or log in an
 * existing member using {@link #login}.
 *
 * <p>The class provides async API with {@link TokenIO} providing a synchronous
 * version. {@link TokenIO} instance can be obtained by calling {@link #sync}
 * method.</p>
 */
public final class TokenIOAsync implements Closeable {
    private static final long SHUTDOWN_DURATION_MS = 10000L;

    private final ManagedChannel channel;
    private final CryptoEngineFactory cryptoFactory;

    /**
     * Creates an instance of a Token SDK.
     *
     * @param channel GRPC channel
     * @param cryptoFactory crypto factory instance
     */
    TokenIOAsync(ManagedChannel channel, CryptoEngineFactory cryptoFactory) {
        this.channel = channel;
        this.cryptoFactory = cryptoFactory;
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
     * Returns a sync version of the API.
     *
     * @return synchronous version of the account API
     */
    public TokenIO sync() {
        return new TokenIO(this);
    }

    /**
     * Checks if a given username already exists.
     *
     * @param username username to check
     * @return {@code true} if username exists, {@code false} otherwise
     */
    public Observable<Boolean> usernameExists(String username) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.usernameExists(username);
    }

    /**
     * Looks up member id for a given username.
     *
     * @param username username to check
     * @return member id if username already exists, null otherwise
     */
    public Observable<String> getMemberId(String username) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.getMemberId(username);
    }

    /**
     * Creates a new Token member with a set of auto-generated keys and a given username.
     *
     * @param username member username to use, must be unique
     * @return newly created member
     */
    public Observable<MemberAsync> createMember(final String username) {
        final UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated
                .createMemberId()
                .flatMap(new Func1<String, Observable<MemberProtos.Member>>() {
                    public Observable<MemberProtos.Member> call(String memberId) {
                        CryptoEngine crypto = cryptoFactory.create(memberId);
                        List<MemberOperation> operations = asList(
                                toAddKeyOperation(crypto.generateKey(PRIVILEGED)),
                                toAddKeyOperation(crypto.generateKey(STANDARD)),
                                toAddKeyOperation(crypto.generateKey(LOW)),
                                toAddUsernameOperation(username));

                        Signer signer = crypto.createSigner(PRIVILEGED);
                        return unauthenticated.createMember(memberId, operations, signer);
                    }
                })
                .flatMap(new Func1<MemberProtos.Member, Observable<MemberAsync>>() {
                    public Observable<MemberAsync> call(MemberProtos.Member member) {
                        CryptoEngine crypto = cryptoFactory.create(member.getId());
                        Client client = ClientFactory.authenticated(
                                channel,
                                member.getId(),
                                crypto);
                        return Observable.just(new MemberAsync(member, client));
                    }
                });
    }

    /**
     * Provisions a new device for an existing user. The call generates a set
     * of keys that are returned back. The keys need to be approved by an
     * existing device/keys.
     *
     * @param username member id to provision the device for
     * @return device information
     */
    public Observable<DeviceInfo> provisionDevice(String username) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated
                .getMemberId(username)
                .map(new Func1<String, DeviceInfo>() {
                    public DeviceInfo call(String memberId) {
                        if (memberId == null) {
                            throw new StatusRuntimeException(NOT_FOUND);
                        }
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
     * Logs in an existing member to the system.
     *
     * @param memberId member id
     * @return logged in member
     */
    public Observable<MemberAsync> login(String memberId) {
        CryptoEngine crypto = cryptoFactory.create(memberId);
        final Client client = ClientFactory.authenticated(channel, memberId, crypto);
        return client
                .getMember(memberId)
                .map(new Func1<MemberProtos.Member, MemberAsync>() {
                    public MemberAsync call(MemberProtos.Member member) {
                        return new MemberAsync(member, client);
                    }
                });
    }

    /**
     * Notifies to link an account.
     *
     * @param username username to notify
     * @param authorization the bank authorization for the funding account
     * @return status of the notification
     */
    public Observable<NotifyStatus> notifyLinkAccounts(
            String username,
            BankAuthorization authorization) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated
                .notifyLinkAccounts(username, authorization);
    }

    /**
     * Notifies to add a key.
     *
     * @param username username to notify
     * @param name device/client name, e.g. iPhone, Chrome Browser, etc
     * @param key key that needs an approval
     * @return status of the notification
     */
    public Observable<NotifyStatus> notifyAddKey(
            String username,
            String name,
            Key key) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.notifyAddKey(username, name, key);
    }

    /**
     * Notifies to link accounts and add a key.
     *
     * @param username username to notify
     * @param authorization the bank authorization for the funding account
     * @param name device/client name, e.g. iPhone, Chrome Browser, etc
     * @param key the that needs an approval
     * @return status of the notification
     */
    public Observable<NotifyStatus> notifyLinkAccountsAndAddKey(
            String username,
            BankAuthorization authorization,
            String name,
            Key key) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.notifyLinkAccountsAndAddKey(
                username,
                authorization,
                name,
                key);
    }

    /**
     * Sends a notification to request a payment.
     *
     * @param username the username of the member to notify
     * @param tokenPayload the payload of a token to be sent
     * @return status of the notification request
     */
    public Observable<NotifyStatus> notifyPaymentRequest(
            String username,
            TokenPayload tokenPayload) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.notifyPaymentRequest(username, tokenPayload);
    }
}
