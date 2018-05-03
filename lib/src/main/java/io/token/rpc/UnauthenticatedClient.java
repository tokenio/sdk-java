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

package io.token.rpc;

import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.util.Util.TOKEN;
import static io.token.util.Util.generateNonce;
import static io.token.util.Util.normalizeAlias;
import static io.token.util.Util.toObservable;

import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.token.TokenRequest;
import io.token.exceptions.MemberNotFoundException;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.bank.BankProtos.Bank;
import io.token.proto.common.member.MemberProtos.Member;
import io.token.proto.common.member.MemberProtos.MemberAddKeyOperation;
import io.token.proto.common.member.MemberProtos.MemberOperation;
import io.token.proto.common.member.MemberProtos.MemberOperationMetadata;
import io.token.proto.common.member.MemberProtos.MemberRecoveryOperation;
import io.token.proto.common.member.MemberProtos.MemberRecoveryOperation.Authorization;
import io.token.proto.common.member.MemberProtos.MemberType;
import io.token.proto.common.member.MemberProtos.MemberUpdate;
import io.token.proto.common.notification.NotificationProtos.AddKey;
import io.token.proto.common.notification.NotificationProtos.LinkAccounts;
import io.token.proto.common.notification.NotificationProtos.LinkAccountsAndAddKey;
import io.token.proto.common.notification.NotificationProtos.NotifyBody;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.security.SecurityProtos.Signature;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.proto.gateway.Gateway.BeginRecoveryRequest;
import io.token.proto.gateway.Gateway.BeginRecoveryResponse;
import io.token.proto.gateway.Gateway.CompleteRecoveryRequest;
import io.token.proto.gateway.Gateway.CompleteRecoveryResponse;
import io.token.proto.gateway.Gateway.CreateMemberRequest;
import io.token.proto.gateway.Gateway.CreateMemberResponse;
import io.token.proto.gateway.Gateway.GetBanksRequest;
import io.token.proto.gateway.Gateway.GetBanksResponse;
import io.token.proto.gateway.Gateway.GetMemberRequest;
import io.token.proto.gateway.Gateway.GetMemberResponse;
import io.token.proto.gateway.Gateway.GetTokenIdRequest;
import io.token.proto.gateway.Gateway.GetTokenIdResponse;
import io.token.proto.gateway.Gateway.NotifyRequest;
import io.token.proto.gateway.Gateway.NotifyResponse;
import io.token.proto.gateway.Gateway.RequestTransferRequest;
import io.token.proto.gateway.Gateway.RequestTransferResponse;
import io.token.proto.gateway.Gateway.ResolveAliasRequest;
import io.token.proto.gateway.Gateway.ResolveAliasResponse;
import io.token.proto.gateway.Gateway.RetrieveTokenRequestRequest;
import io.token.proto.gateway.Gateway.RetrieveTokenRequestResponse;
import io.token.proto.gateway.Gateway.UpdateMemberRequest;
import io.token.proto.gateway.Gateway.UpdateMemberResponse;
import io.token.proto.gateway.GatewayServiceGrpc.GatewayServiceFutureStub;
import io.token.rpc.util.Converters;
import io.token.security.CryptoEngine;
import io.token.security.Signer;

import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Similar to {@link Client} but is only used for a handful of requests that
 * don't require authentication. We use this client to create new member or
 * getMember an existing one and switch to the authenticated {@link Client}.
 */
public final class UnauthenticatedClient {
    private final GatewayServiceFutureStub gateway;

    /**
     * Creates an instance.
     *
     * @param gateway gateway gRPC stub
     */
    public UnauthenticatedClient(GatewayServiceFutureStub gateway) {
        this.gateway = gateway;
    }

    /**
     * Checks if a given alias already exists.
     *
     * @param alias alias to check
     * @return {@code true} if alias already exists, {@code false} otherwise
     */
    public Observable<Boolean> aliasExists(Alias alias) {
        return toObservable(gateway
                .resolveAlias(ResolveAliasRequest
                        .newBuilder()
                        .setAlias(alias)
                        .build()))
                .map(new Function<ResolveAliasResponse, Boolean>() {
                    public Boolean apply(ResolveAliasResponse response) {
                        return response.hasMember();
                    }
                });
    }

    /**
     * Looks up member id for a given alias.
     *
     * @param alias alias to check
     * @return member id, or throws exception if member not found
     */
    public Observable<String> getMemberId(final Alias alias) {
        return toObservable(
                gateway.resolveAlias(ResolveAliasRequest.newBuilder()
                        .setAlias(alias)
                        .build()))
                .map(new Function<ResolveAliasResponse, String>() {
                    public String apply(ResolveAliasResponse response) {
                        if (response.hasMember()) {
                            return response.getMember().getId();
                        } else {
                            throw new MemberNotFoundException(alias);
                        }
                    }
                });
    }

    /**
     * Looks up member information for the given member ID. The user is defined by
     * the key used for authentication.
     *
     * @param memberId member id
     * @return an observable of member
     */
    public Observable<Member> getMember(String memberId) {
        return Converters
                .toObservable(gateway.getMember(GetMemberRequest.newBuilder()
                        .setMemberId(memberId)
                        .build()))
                .map(new Function<GetMemberResponse, Member>() {
                    public Member apply(GetMemberResponse response) {
                        return response.getMember();
                    }
                });
    }

    /**
     * Creates new member ID. After the method returns the ID is reserved on the server.
     *
     * @param memberType the type of member to register
     * @return newly created member id
     */
    public Observable<String> createMemberId(MemberType memberType) {
        return
                toObservable(gateway.createMember(CreateMemberRequest.newBuilder()
                        .setNonce(generateNonce())
                        .setMemberType(memberType)
                        .build()))
                        .map(new Function<CreateMemberResponse, String>() {
                            public String apply(CreateMemberResponse response) {
                                return response.getMemberId();
                            }
                        });
    }

    /**
     * Creates a new token member.
     *
     * @param memberId member ID
     * @param operations operations to set up member keys and aliases
     * @param metadata metadata of operations
     * @param signer the signer used to sign the requests
     * @return member information
     */
    public Observable<Member> createMember(
            String memberId,
            List<MemberOperation> operations,
            List<MemberOperationMetadata> metadata,
            Signer signer) {
        MemberUpdate.Builder update = MemberUpdate.newBuilder()
                .setMemberId(memberId)
                .addAllOperations(operations);
        return toObservable(gateway.updateMember(UpdateMemberRequest.newBuilder()
                .setUpdate(update)
                .setUpdateSignature(Signature.newBuilder()
                        .setMemberId(memberId)
                        .setKeyId(signer.getKeyId())
                        .setSignature(signer.sign(update.build())))
                .addAllMetadata(metadata)
                .build()))
                .map(new Function<UpdateMemberResponse, Member>() {
                    public Member apply(UpdateMemberResponse response) {
                        return response.getMember();
                    }
                });
    }

    /**
     * Retrieves a transfer token request.
     *
     * @param tokenRequestId token request id
     *
     * @return TokenRequest representing the request that was stored with the request id
     */
    public Observable<TokenRequest> retrieveTokenRequest(String tokenRequestId) {
        return toObservable(gateway.retrieveTokenRequest(RetrieveTokenRequestRequest.newBuilder()
                .setRequestId(tokenRequestId)
                .build()))
                .map(new Function<RetrieveTokenRequestResponse, TokenRequest>() {
                    @Override
                    public TokenRequest apply(
                            RetrieveTokenRequestResponse retrieveTokenRequestResponse)
                            throws Exception {
                        return TokenRequest.create(
                                retrieveTokenRequestResponse.getTokenRequest().getPayload(),
                                retrieveTokenRequestResponse.getTokenRequest().getOptionsMap());
                    }
                });
    }

    /**
     * Notifies subscribed devices that accounts should be linked.
     *
     * @param alias alias of the member
     * @param authorization the bank authorization for the funding account
     * @return status status of the notification
     */
    public Observable<NotifyStatus> notifyLinkAccounts(
            Alias alias,
            BankAuthorization authorization) {
        return toObservable(gateway.notify(
                NotifyRequest.newBuilder()
                        .setAlias(alias)
                        .setBody(NotifyBody.newBuilder()
                                .setLinkAccounts(LinkAccounts.newBuilder()
                                        .setBankAuthorization(authorization)
                                        .build())
                                .build())
                        .build()))
                .map(new Function<NotifyResponse, NotifyStatus>() {
                    public NotifyStatus apply(NotifyResponse response) {
                        return response.getStatus();
                    }
                });
    }


    /**
     * Notifies subscribed devices that a key should be added.
     *
     * @param alias alias of the member
     * @param name device/client name, e.g. iPhone, Chrome Browser, etc
     * @param key the that needs an approval
     * @return status status of the notification
     */
    public Observable<NotifyStatus> notifyAddKey(
            Alias alias,
            String name,
            Key key) {
        return toObservable(gateway.notify(
                NotifyRequest.newBuilder()
                        .setAlias(alias)
                        .setBody(NotifyBody.newBuilder()
                                .setAddKey(AddKey.newBuilder()
                                        .setName(name)
                                        .setKey(key)
                                        .build())
                                .build())
                        .build()))
                .map(new Function<NotifyResponse, NotifyStatus>() {
                    public NotifyStatus apply(NotifyResponse response) {
                        return response.getStatus();
                    }
                });
    }

    /**
     * Notifies subscribed devices that a key should be added.
     *
     * @param alias alias of the member
     * @param authorization the bank authorization for the funding account
     * @param name device/client name, e.g. iPhone, Chrome Browser, etc
     * @param key the that needs an approval
     * @return status status of the notification
     */
    public Observable<NotifyStatus> notifyLinkAccountsAndAddKey(
            Alias alias,
            BankAuthorization authorization,
            String name,
            Key key) {
        return toObservable(gateway.notify(
                NotifyRequest.newBuilder()
                        .setAlias(alias)
                        .setBody(NotifyBody.newBuilder()
                                .setLinkAccountsAndAddKey(LinkAccountsAndAddKey.newBuilder()
                                        .setLinkAccounts(LinkAccounts.newBuilder()
                                                .setBankAuthorization(authorization)
                                                .build())
                                        .setAddKey(AddKey.newBuilder()
                                                .setName(name)
                                                .setKey(key)
                                                .build())
                                        .build())
                                .build())
                        .build()))
                .map(new Function<NotifyResponse, NotifyStatus>() {
                    public NotifyStatus apply(NotifyResponse response) {
                        return response.getStatus();
                    }
                });
    }

    /**
     * Notifies subscribed devices of payment requests.
     *
     * @param tokenPayload the payload of a token to be sent
     * @return status of the notification request
     */
    public Observable<NotifyStatus> notifyPaymentRequest(TokenPayload tokenPayload) {
        return toObservable(gateway.requestTransfer(
                RequestTransferRequest.newBuilder()
                        .setTokenPayload(tokenPayload)
                        .build()))
                .map(new Function<RequestTransferResponse, NotifyStatus>() {
                    public NotifyStatus apply(RequestTransferResponse response) {
                        return response.getStatus();
                    }
                });
    }

    /**
     * Notifies subscribed devices of payment requests.
     *
     * @param alias the alias of the member to notify
     * @param tokenPayload the payload of a token to be sent
     * @return status of the notification request
     * @deprecated use version without alias parameter
     */
    @Deprecated
    public Observable<NotifyStatus> notifyPaymentRequest(Alias alias, TokenPayload tokenPayload) {
        return notifyPaymentRequest(tokenPayload);
    }

    /**
     * Begins account recovery.
     *
     * @param alias the alias used to recover
     * @return the verification id
     */
    public Observable<String> beginRecovery(Alias alias) {
        return toObservable(gateway
                .beginRecovery(BeginRecoveryRequest.newBuilder()
                        .setAlias(normalizeAlias(alias))
                        .build()))
                .map(new Function<BeginRecoveryResponse, String>() {
                    public String apply(BeginRecoveryResponse response) {
                        return response.getVerificationId();
                    }
                });
    }

    /**
     * Create a recovery authorization for some agent to sign.
     *
     * @param memberId Id of member we claim to be.
     * @param privilegedKey new privileged key we want to use.
     * @return authorization structure for agent to sign
     */
    public Observable<Authorization> createRecoveryAuthorization(
            final String memberId,
            final Key privilegedKey) {
        return toObservable(gateway.getMember(GetMemberRequest.newBuilder()
                .setMemberId(memberId)
                .build()))
                .map(new Function<GetMemberResponse, Authorization>() {
                    public Authorization apply(GetMemberResponse response) throws Exception {
                        return Authorization.newBuilder()
                                .setMemberId(memberId)
                                .setMemberKey(privilegedKey)
                                .setPrevHash(response.getMember().getLastHash())
                                .build();
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
     * @return an observable of the updatedMember
     */
    public Observable<Member> completeRecovery(
            final String memberId,
            final List<MemberRecoveryOperation> recoveryOperations,
            final Key privilegedKey,
            final CryptoEngine cryptoEngine) {
        final Key standardKey = cryptoEngine.generateKey(STANDARD);
        final Key lowKey = cryptoEngine.generateKey(LOW);
        final Signer signer = cryptoEngine.createSigner(PRIVILEGED);
        final List<MemberOperation> operations = new LinkedList<>();
        for (MemberRecoveryOperation op : recoveryOperations) {
            operations.add(MemberOperation.newBuilder().setRecover(op).build());
        }
        return toObservable(gateway.getMember(GetMemberRequest.newBuilder()
                .setMemberId(memberId)
                .build()))
                .map(new Function<GetMemberResponse, MemberUpdate>() {
                    public MemberUpdate apply(GetMemberResponse response) throws Exception {
                        return MemberUpdate.newBuilder()
                                .setMemberId(memberId)
                                .setPrevHash(response.getMember().getLastHash())
                                .addAllOperations(operations)
                                .addAllOperations(toMemberOperations(
                                        privilegedKey,
                                        standardKey,
                                        lowKey))
                                .build();
                    }
                })
                .flatMap(new Function<MemberUpdate, Observable<Member>>() {
                    public Observable<Member> apply(MemberUpdate memberUpdate) throws Exception {
                        return toObservable(gateway.updateMember(UpdateMemberRequest.newBuilder()
                                .setUpdate(memberUpdate)
                                .setUpdateSignature(Signature.newBuilder()
                                        .setKeyId(signer.getKeyId())
                                        .setMemberId(memberId)
                                        .setSignature(signer.sign(memberUpdate)))
                                .build()))
                                .map(new Function<UpdateMemberResponse, Member>() {
                                    public Member apply(UpdateMemberResponse response) {
                                        return response.getMember();
                                    }
                                });
                    }
                });
    }

    private List<MemberOperation> toMemberOperations(Key... keys) {
        List<MemberOperation> operations = new LinkedList<>();
        for (Key key : keys) {
            operations.add(MemberOperation.newBuilder()
                    .setAddKey(MemberAddKeyOperation.newBuilder()
                            .setKey(key))
                    .build());
        }
        return operations;
    }

    /**
     * Completes account recovery if the default recovery rule was set.
     *
     * @param memberId the member id
     * @param verificationId the verification id
     * @param code the code
     * @param cryptoEngine the new crypto engine
     * @return the recovery entry
     */
    public Observable<Member> completeRecoveryWithDefaultRule(
            final String memberId,
            final String verificationId,
            final String code,
            CryptoEngine cryptoEngine) {
        final Key privilegedKey = cryptoEngine.generateKey(PRIVILEGED);
        final Key standardKey = cryptoEngine.generateKey(STANDARD);
        final Key lowKey = cryptoEngine.generateKey(LOW);
        final Signer signer = cryptoEngine.createSigner(PRIVILEGED);
        return toObservable(gateway
                .completeRecovery(CompleteRecoveryRequest.newBuilder()
                        .setVerificationId(verificationId)
                        .setCode(code)
                        .setKey(privilegedKey)
                        .build()))
                .flatMap(new Function<CompleteRecoveryResponse, Observable<MemberUpdate>>() {
                    public Observable<MemberUpdate> apply(final CompleteRecoveryResponse res) {
                        return toObservable(gateway
                                .getMember(GetMemberRequest.newBuilder()
                                        .setMemberId(memberId)
                                        .build()))
                                .map(new Function<GetMemberResponse, MemberUpdate>() {
                                    public MemberUpdate apply(GetMemberResponse memberRes) {
                                        return MemberUpdate.newBuilder()
                                                .setPrevHash(memberRes.getMember().getLastHash())
                                                .setMemberId(memberId)
                                                .addOperations(MemberOperation.newBuilder()
                                                        .setRecover(res.getRecoveryEntry()))
                                                .addAllOperations(toMemberOperations(
                                                        privilegedKey,
                                                        standardKey,
                                                        lowKey))
                                                .build();
                                    }
                                });
                    }
                })
                .flatMap(new Function<MemberUpdate, Observable<Member>>() {
                    public Observable<Member> apply(MemberUpdate memberUpdate) {
                        return toObservable(gateway.updateMember(UpdateMemberRequest.newBuilder()
                                .setUpdate(memberUpdate)
                                .setUpdateSignature(Signature.newBuilder()
                                        .setKeyId(signer.getKeyId())
                                        .setMemberId(memberId)
                                        .setSignature(signer.sign(memberUpdate)))
                                .build()))
                                .map(new Function<UpdateMemberResponse, Member>() {
                                    public Member apply(UpdateMemberResponse response) {
                                        return response.getMember();
                                    }
                                });
                    }
                });
    }

    /**
     * Gets recovery authorization from Token.
     *
     * @param verificationId the verification id
     * @param code the code
     * @param privilegedKey the privileged key
     * @return the recovery entry
     */
    public Observable<MemberRecoveryOperation> getRecoveryAuthorization(
            String verificationId,
            String code,
            Key privilegedKey) {
        return toObservable(gateway.completeRecovery(CompleteRecoveryRequest.newBuilder()
                .setVerificationId(verificationId)
                .setCode(code)
                .setKey(privilegedKey)
                .build()))
                .map(new Function<CompleteRecoveryResponse, MemberRecoveryOperation>() {
                    public MemberRecoveryOperation apply(CompleteRecoveryResponse response) {
                        return response.getRecoveryEntry();
                    }
                });
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
    public Observable<List<Bank>> getBanks(
            @Nullable List<String> bankIds,
            @Nullable String search,
            @Nullable String country,
            @Nullable Integer page,
            @Nullable Integer perPage,
            @Nullable String sort) {
        GetBanksRequest.Builder request = GetBanksRequest.newBuilder();

        if (bankIds != null) {
            request.addAllIds(bankIds);
        }
        if (search != null) {
            request.setSearch(search);
        }
        if (country != null) {
            request.setCountry(country);
        }
        if (page != null) {
            request.setPage(page);
        }
        if (perPage != null) {
            request.setPerPage(perPage);
        }
        if (sort != null) {
            request.setSort(sort);
        }

        return toObservable(gateway.getBanks(request.build()))
                .map(new Function<GetBanksResponse, List<Bank>>() {
                    public List<Bank> apply(GetBanksResponse response) {
                        return response.getBanksList();
                    }
                });
    }

    /**
     * Return the token member.
     *
     * @return token member
     */
    public Observable<Member> getTokenMember() {
        return getMemberId(TOKEN).flatMap(
                new Function<String, Observable<Member>>() {
                    @Override
                    public Observable<Member> apply(String memberId) throws Exception {
                        return getMember(memberId);
                    }
                });
    }

    /**
     * Get a token ID based on a token's tokenRequestId.
     *
     * @param tokenRequestId token request id
     * @return token id
     */
    public Observable<String> getTokenId(String tokenRequestId) {
        return toObservable(gateway
                .getTokenId(GetTokenIdRequest.newBuilder()
                        .setTokenRequestId(tokenRequestId)
                        .build()))
                .map(new Function<GetTokenIdResponse, String>() {
                    @Override
                    public String apply(GetTokenIdResponse response) throws Exception {
                        return response.getTokenId();
                    }
                });
    }
}
