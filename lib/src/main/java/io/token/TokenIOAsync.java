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
import static io.token.TokenIO.TokenCluster;
import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.util.Util.generateNonce;
import static io.token.util.Util.hashString;
import static io.token.util.Util.normalizeAlias;
import static io.token.util.Util.toAddAliasOperation;
import static io.token.util.Util.toAddAliasOperationMetadata;
import static io.token.util.Util.toAddKeyOperation;
import static io.token.util.Util.verifySignature;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.token.exceptions.InvalidStateException;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.bank.BankProtos.Bank;
import io.token.proto.common.member.MemberProtos;
import io.token.proto.common.member.MemberProtos.Member;
import io.token.proto.common.member.MemberProtos.MemberOperation;
import io.token.proto.common.member.MemberProtos.MemberOperationMetadata;
import io.token.proto.common.member.MemberProtos.MemberRecoveryOperation;
import io.token.proto.common.member.MemberProtos.MemberRecoveryOperation.Authorization;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.rpc.Client;
import io.token.rpc.ClientFactory;
import io.token.rpc.UnauthenticatedClient;
import io.token.security.CryptoEngine;
import io.token.security.CryptoEngineFactory;
import io.token.security.InMemoryKeyStore;
import io.token.security.Signer;
import io.token.security.TokenCryptoEngine;
import io.token.tokenrequest.TokenRequestCallbackParameters;
import io.token.tokenrequest.TokenRequestState;

import java.io.Closeable;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * An SDK Client that interacts with TokenOS.
 *
 * <p>The class provides async API with {@link TokenIO} providing a synchronous
 * version. {@link TokenIO} instance can be obtained by calling {@link #sync}
 * method.</p>
 */
public class TokenIOAsync implements Closeable {
    private static final String TOKEN_REQUEST_TEMPLATE =
            "https://%s/authorize?requestId=%s&state=%s";
    private static final long SHUTDOWN_DURATION_MS = 10000L;

    private final ManagedChannel channel;
    private final CryptoEngineFactory cryptoFactory;
    private final String devKey;
    private TokenCluster tokenCluster;

    /**
     * Creates an instance of a Token SDK.
     *
     * @param channel GRPC channel
     * @param cryptoFactory crypto factory instance
     * @param developerKey developer key
     * @param tokenCluster token cluster
     */
    TokenIOAsync(
            ManagedChannel channel,
            CryptoEngineFactory cryptoFactory,
            String developerKey,
            TokenCluster tokenCluster) {
        this.channel = channel;
        this.cryptoFactory = cryptoFactory;
        this.devKey = developerKey;
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
     * Returns a sync version of the API.
     *
     * @return synchronous version of the account API
     */
    public TokenIO sync() {
        return new TokenIO(this, devKey);
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
     * Looks up member id for a given alias.
     *
     * @param alias alias to check
     * @return member id if alias already exists, null otherwise
     */
    public Observable<String> getMemberId(Alias alias) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.getMemberId(alias);
    }

    /**
     * Creates a new Token member with a set of auto-generated keys and a alias.
     *
     * @param alias nullable member alias to use, must be unique. If null, then no alias will
     *     be created with the member.
     * @return newly created member
     */
    public Observable<MemberAsync> createMember(final Alias alias) {
        final UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated
                .createMemberId()
                .flatMap(new Function<String, Observable<MemberProtos.Member>>() {
                    public Observable<MemberProtos.Member> apply(String memberId) {
                        CryptoEngine crypto = cryptoFactory.create(memberId);
                        List<MemberOperation> operations = new ArrayList<>();
                        operations.add(toAddKeyOperation(crypto.generateKey(PRIVILEGED)));
                        operations.add(toAddKeyOperation(crypto.generateKey(STANDARD)));
                        operations.add(toAddKeyOperation(crypto.generateKey(LOW)));
                        if (alias != null) {
                            operations.add(toAddAliasOperation(normalizeAlias(alias)));
                        }
                        List<MemberOperationMetadata> metadata = alias == null
                                ? Collections.<MemberOperationMetadata>emptyList()
                                : singletonList(toAddAliasOperationMetadata(normalizeAlias(alias)));
                        Signer signer = crypto.createSigner(PRIVILEGED);
                        return unauthenticated.createMember(memberId, operations, metadata, signer);
                    }
                })
                .flatMap(new Function<MemberProtos.Member, Observable<MemberAsync>>() {
                    public Observable<MemberAsync> apply(MemberProtos.Member member) {
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
     * Creates a new Token member with a set of auto-generated keys and no alias.
     *
     * @return newly created member
     */
    public Observable<MemberAsync> createMember() {
        return createMember(null);
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
     * Return a Member set up to use some Token member's keys (assuming we have them).
     *
     * @param memberId member id
     * @return member
     */
    public Observable<MemberAsync> getMember(String memberId) {
        CryptoEngine crypto = cryptoFactory.create(memberId);
        final Client client = ClientFactory.authenticated(channel, memberId, crypto);
        return client
                .getMember(memberId)
                .map(new Function<MemberProtos.Member, MemberAsync>() {
                    public MemberAsync apply(MemberProtos.Member member) {
                        return new MemberAsync(member, client);
                    }
                });
    }

    /**
     * Return a TokenRequest that was previously stored.
     *
     * @param requestId request id
     * @return token request
     */
    public Observable<TokenRequest> retrieveTokenRequest(String requestId) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.retrieveTokenRequest(requestId);
    }

    /**
     * Return a Member set up to use some Token member's keys (assuming we have them).
     *
     * @param memberId member id
     * @return member
     * @deprecated login's name changed to getMember
     */
    @Deprecated
    public Observable<MemberAsync> login(String memberId) {
        CryptoEngine crypto = cryptoFactory.create(memberId);
        final Client client = ClientFactory.authenticated(channel, memberId, crypto);
        return client
                .getMember(memberId)
                .map(new Function<MemberProtos.Member, MemberAsync>() {
                    public MemberAsync apply(MemberProtos.Member member) {
                        return new MemberAsync(member, client);
                    }
                });
    }

    /**
     * Notifies to link an account.
     *
     * @param alias alias to notify
     * @param authorization the bank authorization for the funding account
     * @return status of the notification
     */
    public Observable<NotifyStatus> notifyLinkAccounts(
            Alias alias,
            BankAuthorization authorization) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated
                .notifyLinkAccounts(alias, authorization);
    }

    /**
     * Notifies to add a key.
     *
     * @param alias alias to notify
     * @param name device/client name, e.g. iPhone, Chrome Browser, etc
     * @param key key that needs an approval
     * @return status of the notification
     */
    public Observable<NotifyStatus> notifyAddKey(
            Alias alias,
            String name,
            Key key) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.notifyAddKey(alias, name, key);
    }

    /**
     * Notifies to link accounts and add a key.
     *
     * @param alias alias to notify
     * @param authorization the bank authorization for the funding account
     * @param name device/client name, e.g. iPhone, Chrome Browser, etc
     * @param key the that needs an approval
     * @return status of the notification
     */
    public Observable<NotifyStatus> notifyLinkAccountsAndAddKey(
            Alias alias,
            BankAuthorization authorization,
            String name,
            Key key) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.notifyLinkAccountsAndAddKey(
                alias,
                authorization,
                name,
                key);
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
     * Gets recovery authorization from Token.
     *
     * @param verificationId the verification id
     * @param code the code
     * @param key the privileged key
     * @return the member recovery operation
     */
    public Observable<MemberRecoveryOperation> getRecoveryAuthorization(
            String verificationId,
            String code,
            Key key) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.getRecoveryAuthorization(verificationId, code, key);
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
    public Observable<MemberAsync> completeRecovery(
            String memberId,
            List<MemberRecoveryOperation> recoveryOperations,
            Key privilegedKey,
            final CryptoEngine cryptoEngine) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated
                .completeRecovery(memberId, recoveryOperations, privilegedKey, cryptoEngine)
                .map(new Function<MemberProtos.Member, MemberAsync>() {
                    public MemberAsync apply(MemberProtos.Member member) throws Exception {
                        Client client = ClientFactory.authenticated(
                                channel,
                                member.getId(),
                                cryptoEngine);
                        return new MemberAsync(member, client);
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
    public Observable<MemberAsync> completeRecoveryWithDefaultRule(
            String memberId,
            String verificationId,
            String code) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        final CryptoEngine cryptoEngine = new TokenCryptoEngine(memberId, new InMemoryKeyStore());
        return unauthenticated
                .completeRecoveryWithDefaultRule(memberId, verificationId, code, cryptoEngine)
                .map(new Function<MemberProtos.Member, MemberAsync>() {
                    public MemberAsync apply(MemberProtos.Member member) throws Exception {
                        Client client = ClientFactory.authenticated(
                                channel,
                                member.getId(),
                                cryptoEngine);
                        return new MemberAsync(member, client);
                    }
                });
    }


    /**
     * Returns a list of token enabled banks.
     *
     * @return a list of banks
     */
    public Observable<List<Bank>> getBanks() {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.getBanks();
    }

    /**
     * Generate a Token request URL from a request ID, an original state, a CSRF token and a token
     * cluster.
     *
     * @param requestId request id
     * @param state state
     * @param csrfToken csrf token
     * @return token request url
     */
    public Observable<String> generateTokenRequestUrl(
            String requestId,
            String state,
            String csrfToken) {
        try {
            String csrfTokenHash = hashString(csrfToken);
            TokenRequestState tokenRequestState = TokenRequestState.create(csrfTokenHash, state);
            return Observable.just(format(TOKEN_REQUEST_TEMPLATE,
                            tokenCluster.webAppUrl(),
                            requestId,
                            URLEncoder.encode(tokenRequestState.serialize(), "UTF-8")));
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex.getCause());
        }
    }

    /**
     * Parse the token request callback URL to extract the state, the token ID and the signature of
     * (state | token ID). Verify that the state contains the csrf token's hash, and that the
     * signature of the token request payload is valid.
     *
     * @param callbackUrl token request callback url
     * @param csrfToken csrfToken
     * @return TokenRequestCallback object containing the token id and the original state
     */
    public Observable<TokenRequestCallback> parseTokenRequestCallbackUrl(
            final String callbackUrl,
            final String csrfToken) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.getTokenMember().map(new Function<Member, TokenRequestCallback>() {
            @Override
            public TokenRequestCallback apply(Member tokenMember) throws Exception {
                TokenRequestCallbackParameters params = TokenRequestCallbackParameters
                        .create(new URL(callbackUrl).getQuery());

                // check that csrf token hashes match
                TokenRequestState state = TokenRequestState.parse(params.getSerializedState());
                if (!state.getCsrfTokenHash().equals(hashString(csrfToken))) {
                    throw new InvalidStateException(csrfToken);
                }

                verifySignature(
                        tokenMember,
                        TokenProtos.RequestSignaturePayload.newBuilder()
                                .setTokenId(params.getTokenId())
                                .setState(params.getSerializedState())
                                .build(),
                        params.getSignature());

                return TokenRequestCallback.create(params.getTokenId(), state.getInnerState());
            }
        });
    }
}
