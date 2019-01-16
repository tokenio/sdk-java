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

import static com.google.common.base.Strings.nullToEmpty;
import static io.token.proto.ProtoJson.toJson;
import static io.token.proto.banklink.Banklink.AccountLinkingStatus.FAILURE_BANK_AUTHORIZATION_REQUIRED;
import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.proto.common.token.TokenProtos.TokenSignature.Action.CANCELLED;
import static io.token.proto.common.token.TokenProtos.TokenSignature.Action.ENDORSED;
import static io.token.proto.common.transaction.TransactionProtos.RequestStatus.SUCCESSFUL_REQUEST;
import static io.token.rpc.util.Converters.toCompletable;
import static io.token.util.Util.toObservable;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.token.TransferTokenException;
import io.token.exceptions.BankAuthorizationRequiredException;
import io.token.exceptions.StepUpRequiredException;
import io.token.proto.PagedList;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.proto.banklink.Banklink.OauthBankAuthorization;
import io.token.proto.common.account.AccountProtos.Account;
import io.token.proto.common.address.AddressProtos.Address;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.bank.BankProtos.BankInfo;
import io.token.proto.common.blob.BlobProtos.Blob;
import io.token.proto.common.blob.BlobProtos.Blob.Payload;
import io.token.proto.common.member.MemberProtos.AddressRecord;
import io.token.proto.common.member.MemberProtos.Device;
import io.token.proto.common.member.MemberProtos.Member;
import io.token.proto.common.member.MemberProtos.MemberOperation;
import io.token.proto.common.member.MemberProtos.MemberOperationMetadata;
import io.token.proto.common.member.MemberProtos.MemberRecoveryOperation.Authorization;
import io.token.proto.common.member.MemberProtos.MemberRecoveryRulesOperation;
import io.token.proto.common.member.MemberProtos.MemberUpdate;
import io.token.proto.common.member.MemberProtos.Profile;
import io.token.proto.common.member.MemberProtos.ProfilePictureSize;
import io.token.proto.common.member.MemberProtos.ReceiptContact;
import io.token.proto.common.member.MemberProtos.RecoveryRule;
import io.token.proto.common.member.MemberProtos.TrustedBeneficiary;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.notification.NotificationProtos;
import io.token.proto.common.notification.NotificationProtos.Notification;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.notification.NotificationProtos.StepUp;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.security.SecurityProtos.SecurityMetadata;
import io.token.proto.common.security.SecurityProtos.Signature;
import io.token.proto.common.subscriber.SubscriberProtos.Subscriber;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.proto.common.token.TokenProtos.TokenRequestOptions;
import io.token.proto.common.token.TokenProtos.TokenRequestPayload;
import io.token.proto.common.token.TokenProtos.TokenRequestStatePayload;
import io.token.proto.common.token.TokenProtos.TokenSignature.Action;
import io.token.proto.common.token.TokenProtos.TransferTokenStatus;
import io.token.proto.common.transaction.TransactionProtos.Balance;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.proto.common.transfer.TransferProtos.TransferPayload;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferEndpoint;
import io.token.proto.gateway.Gateway.AddAddressRequest;
import io.token.proto.gateway.Gateway.AddAddressResponse;
import io.token.proto.gateway.Gateway.AddTrustedBeneficiaryRequest;
import io.token.proto.gateway.Gateway.ApplyScaRequest;
import io.token.proto.gateway.Gateway.CancelTokenRequest;
import io.token.proto.gateway.Gateway.CancelTokenResponse;
import io.token.proto.gateway.Gateway.CreateAccessTokenRequest;
import io.token.proto.gateway.Gateway.CreateAccessTokenResponse;
import io.token.proto.gateway.Gateway.CreateBlobRequest;
import io.token.proto.gateway.Gateway.CreateBlobResponse;
import io.token.proto.gateway.Gateway.CreateCustomizationRequest;
import io.token.proto.gateway.Gateway.CreateCustomizationResponse;
import io.token.proto.gateway.Gateway.CreateTestBankAccountRequest;
import io.token.proto.gateway.Gateway.CreateTestBankAccountResponse;
import io.token.proto.gateway.Gateway.CreateTransferRequest;
import io.token.proto.gateway.Gateway.CreateTransferResponse;
import io.token.proto.gateway.Gateway.CreateTransferTokenRequest;
import io.token.proto.gateway.Gateway.CreateTransferTokenResponse;
import io.token.proto.gateway.Gateway.DeleteAddressRequest;
import io.token.proto.gateway.Gateway.DeleteMemberRequest;
import io.token.proto.gateway.Gateway.EndorseTokenRequest;
import io.token.proto.gateway.Gateway.EndorseTokenResponse;
import io.token.proto.gateway.Gateway.GetAccountRequest;
import io.token.proto.gateway.Gateway.GetAccountResponse;
import io.token.proto.gateway.Gateway.GetAccountsRequest;
import io.token.proto.gateway.Gateway.GetAccountsResponse;
import io.token.proto.gateway.Gateway.GetActiveAccessTokenRequest;
import io.token.proto.gateway.Gateway.GetActiveAccessTokenResponse;
import io.token.proto.gateway.Gateway.GetAddressRequest;
import io.token.proto.gateway.Gateway.GetAddressResponse;
import io.token.proto.gateway.Gateway.GetAddressesRequest;
import io.token.proto.gateway.Gateway.GetAddressesResponse;
import io.token.proto.gateway.Gateway.GetAliasesRequest;
import io.token.proto.gateway.Gateway.GetAliasesResponse;
import io.token.proto.gateway.Gateway.GetBalanceRequest;
import io.token.proto.gateway.Gateway.GetBalanceResponse;
import io.token.proto.gateway.Gateway.GetBalancesRequest;
import io.token.proto.gateway.Gateway.GetBalancesResponse;
import io.token.proto.gateway.Gateway.GetBankInfoRequest;
import io.token.proto.gateway.Gateway.GetBankInfoResponse;
import io.token.proto.gateway.Gateway.GetBlobRequest;
import io.token.proto.gateway.Gateway.GetBlobResponse;
import io.token.proto.gateway.Gateway.GetDefaultAccountRequest;
import io.token.proto.gateway.Gateway.GetDefaultAccountResponse;
import io.token.proto.gateway.Gateway.GetDefaultAgentRequest;
import io.token.proto.gateway.Gateway.GetDefaultAgentResponse;
import io.token.proto.gateway.Gateway.GetMemberRequest;
import io.token.proto.gateway.Gateway.GetMemberResponse;
import io.token.proto.gateway.Gateway.GetNotificationRequest;
import io.token.proto.gateway.Gateway.GetNotificationResponse;
import io.token.proto.gateway.Gateway.GetNotificationsRequest;
import io.token.proto.gateway.Gateway.GetNotificationsResponse;
import io.token.proto.gateway.Gateway.GetPairedDevicesRequest;
import io.token.proto.gateway.Gateway.GetPairedDevicesResponse;
import io.token.proto.gateway.Gateway.GetProfilePictureRequest;
import io.token.proto.gateway.Gateway.GetProfilePictureResponse;
import io.token.proto.gateway.Gateway.GetProfileRequest;
import io.token.proto.gateway.Gateway.GetProfileResponse;
import io.token.proto.gateway.Gateway.GetReceiptContactRequest;
import io.token.proto.gateway.Gateway.GetReceiptContactResponse;
import io.token.proto.gateway.Gateway.GetSubscriberRequest;
import io.token.proto.gateway.Gateway.GetSubscriberResponse;
import io.token.proto.gateway.Gateway.GetSubscribersRequest;
import io.token.proto.gateway.Gateway.GetSubscribersResponse;
import io.token.proto.gateway.Gateway.GetTokenBlobRequest;
import io.token.proto.gateway.Gateway.GetTokenBlobResponse;
import io.token.proto.gateway.Gateway.GetTokenRequest;
import io.token.proto.gateway.Gateway.GetTokenResponse;
import io.token.proto.gateway.Gateway.GetTokensRequest;
import io.token.proto.gateway.Gateway.GetTokensResponse;
import io.token.proto.gateway.Gateway.GetTransactionRequest;
import io.token.proto.gateway.Gateway.GetTransactionResponse;
import io.token.proto.gateway.Gateway.GetTransactionsRequest;
import io.token.proto.gateway.Gateway.GetTransactionsResponse;
import io.token.proto.gateway.Gateway.GetTransferRequest;
import io.token.proto.gateway.Gateway.GetTransferResponse;
import io.token.proto.gateway.Gateway.GetTransfersRequest;
import io.token.proto.gateway.Gateway.GetTransfersResponse;
import io.token.proto.gateway.Gateway.GetTrustedBeneficiariesRequest;
import io.token.proto.gateway.Gateway.GetTrustedBeneficiariesResponse;
import io.token.proto.gateway.Gateway.LinkAccountsOauthRequest;
import io.token.proto.gateway.Gateway.LinkAccountsOauthResponse;
import io.token.proto.gateway.Gateway.LinkAccountsRequest;
import io.token.proto.gateway.Gateway.LinkAccountsResponse;
import io.token.proto.gateway.Gateway.Page;
import io.token.proto.gateway.Gateway.RemoveTrustedBeneficiaryRequest;
import io.token.proto.gateway.Gateway.ReplaceTokenRequest;
import io.token.proto.gateway.Gateway.ReplaceTokenRequest.CancelToken;
import io.token.proto.gateway.Gateway.ReplaceTokenRequest.CreateToken;
import io.token.proto.gateway.Gateway.ReplaceTokenResponse;
import io.token.proto.gateway.Gateway.ResolveTransferDestinationsRequest;
import io.token.proto.gateway.Gateway.ResolveTransferDestinationsResponse;
import io.token.proto.gateway.Gateway.RetryVerificationRequest;
import io.token.proto.gateway.Gateway.RetryVerificationResponse;
import io.token.proto.gateway.Gateway.SetDefaultAccountRequest;
import io.token.proto.gateway.Gateway.SetProfilePictureRequest;
import io.token.proto.gateway.Gateway.SetProfileRequest;
import io.token.proto.gateway.Gateway.SetProfileResponse;
import io.token.proto.gateway.Gateway.SetReceiptContactRequest;
import io.token.proto.gateway.Gateway.SignTokenRequestStateRequest;
import io.token.proto.gateway.Gateway.SignTokenRequestStateResponse;
import io.token.proto.gateway.Gateway.StoreTokenRequestRequest;
import io.token.proto.gateway.Gateway.StoreTokenRequestResponse;
import io.token.proto.gateway.Gateway.SubscribeToNotificationsRequest;
import io.token.proto.gateway.Gateway.SubscribeToNotificationsResponse;
import io.token.proto.gateway.Gateway.TriggerStepUpNotificationRequest;
import io.token.proto.gateway.Gateway.TriggerStepUpNotificationResponse;
import io.token.proto.gateway.Gateway.UnlinkAccountsRequest;
import io.token.proto.gateway.Gateway.UnsubscribeFromNotificationsRequest;
import io.token.proto.gateway.Gateway.UpdateMemberRequest;
import io.token.proto.gateway.Gateway.UpdateMemberResponse;
import io.token.proto.gateway.Gateway.UpdateTokenRequestRequest;
import io.token.proto.gateway.Gateway.VerifyAffiliateRequest;
import io.token.proto.gateway.Gateway.VerifyAliasRequest;
import io.token.proto.gateway.GatewayServiceGrpc.GatewayServiceFutureStub;
import io.token.security.CryptoEngine;
import io.token.security.Signer;
import io.token.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;


/**
 * An authenticated RPC client that is used to talk to Token gateway. The
 * class is a thin wrapper on top of gRPC generated client. Makes the API
 * easier to use.
 */
public final class Client {
    private final String memberId;
    private final CryptoEngine crypto;
    private final GatewayProvider gateway;
    private boolean customerInitiated = false;
    private String onBehalfOf;
    private SecurityMetadata securityMetadata = SecurityMetadata.getDefaultInstance();

    /**
     * Creates a client instance.
     *
     * @param memberId member id
     * @param crypto the crypto engine used to sign for authentication, request payloads, etc
     * @param gateway gateway gRPC stub
     */
    Client(String memberId, CryptoEngine crypto, GatewayProvider gateway) {
        this.memberId = memberId;
        this.crypto = crypto;
        this.gateway = gateway;
    }

    /**
     * Creates a new instance with On-Behalf-Of authentication set.
     *
     * @param tokenId access token ID to be used
     * @param customerInitiated whether the customer initiated the calls
     * @return new client instance
     */
    public Client forAccessToken(String tokenId, boolean customerInitiated) {
        Client updated = new Client(memberId, crypto, gateway);
        updated.useAccessToken(tokenId, customerInitiated);
        updated.setSecurityMetadata(securityMetadata);
        return updated;
    }

    /**
     * Sets the On-Behalf-Of authentication value to be used
     * with this client.  The value must correspond to an existing
     * Access Token ID issued for the client member. Sets customer initiated
     * to false.
     *
     * @param accessTokenId the access token id to be used
     */
    public void useAccessToken(String accessTokenId) {
        useAccessToken(accessTokenId, false);
    }

    /**
     * Sets the On-Behalf-Of authentication value to be used
     * with this client.  The value must correspond to an existing
     * Access Token ID issued for the client member. Uses the given customer
     * initiated flag.
     *
     * @param accessTokenId the access token id to be used
     * @param customerInitiated whether the customer initiated the calls
     */
    public void useAccessToken(String accessTokenId, boolean customerInitiated) {
        this.onBehalfOf = accessTokenId;
        this.customerInitiated = customerInitiated;
    }

    /**
     * Clears the On-Behalf-Of value used with this client.
     */
    @Deprecated
    public void clearAccessToken() {
        this.onBehalfOf = null;
        this.customerInitiated = false;
    }

    /**
     * Looks up member information for the current user. The user is defined by
     * the key used for authentication.
     *
     * @param memberId member id
     * @return an observable of member
     */
    public Observable<Member> getMember(String memberId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getMember(GetMemberRequest.newBuilder()
                        .setMemberId(memberId)
                        .build()))
                .map(new Function<GetMemberResponse, Member>() {
                    public Member apply(GetMemberResponse response) {
                        return response.getMember();
                    }
                });
    }

    /**
     * Updates member by applying the specified operations.
     *
     * @param member member to update
     * @param operations operations to apply
     * @param metadata metadata of operations
     * @return an observable of updated member
     */
    public Observable<Member> updateMember(
            Member member,
            List<MemberOperation> operations,
            List<MemberOperationMetadata> metadata) {
        if (operations.isEmpty()) {
            return Observable.just(member);
        }
        Signer signer = crypto.createSigner(PRIVILEGED);
        MemberUpdate update = MemberUpdate
                .newBuilder()
                .setMemberId(member.getId())
                .setPrevHash(member.getLastHash())
                .addAllOperations(operations)
                .build();

        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .updateMember(UpdateMemberRequest
                        .newBuilder()
                        .setUpdate(update)
                        .setUpdateSignature(Signature
                                .newBuilder()
                                .setMemberId(memberId)
                                .setKeyId(signer.getKeyId())
                                .setSignature(signer.sign(update)))
                        .addAllMetadata(metadata)
                        .build()))
                .map(new Function<UpdateMemberResponse, Member>() {
                    public Member apply(UpdateMemberResponse response) {
                        return response.getMember();
                    }
                });
    }

    /**
     * Updates member by applying the specified operations that don't contain addAliasOperation.
     *
     * @param member member to update
     * @param operations operations to apply
     * @return an observable of updated member
     */
    public Observable<Member> updateMember(Member member, List<MemberOperation> operations) {
        return updateMember(member, operations, Collections.<MemberOperationMetadata>emptyList());
    }

    /**
     * Creates a subscriber to receive push notifications.
     *
     * @param handler specify the handler of the notifications
     * @param handlerInstructions map of instructions for the handler
     * @return notification subscriber
     */
    public Observable<Subscriber> subscribeToNotifications(
            String handler,
            Map<String, String> handlerInstructions) {
        SubscribeToNotificationsRequest request = SubscribeToNotificationsRequest
                .newBuilder()
                .setHandler(handler)
                .putAllHandlerInstructions(handlerInstructions)
                .build();
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .subscribeToNotifications(request))
                .map(new Function<SubscribeToNotificationsResponse, Subscriber>() {
                    public Subscriber apply(SubscribeToNotificationsResponse response) {
                        return response.getSubscriber();
                    }
                });
    }

    /**
     * Set Token as the recovery agent.
     *
     * @return a completable
     */
    public Completable useDefaultRecoveryRule() {
        final Signer signer = crypto.createSigner(PRIVILEGED);
        return getMember(memberId)
                .flatMap(new Function<Member, Observable<MemberUpdate>>() {
                    public Observable<MemberUpdate> apply(final Member member) {
                        return toObservable(gateway
                                .withAuthentication(authenticationContext())
                                .getDefaultAgent(GetDefaultAgentRequest.getDefaultInstance()))
                                .map(new Function<GetDefaultAgentResponse, MemberUpdate>() {
                                    public MemberUpdate apply(GetDefaultAgentResponse response) {
                                        RecoveryRule rule = RecoveryRule.newBuilder()
                                                .setPrimaryAgent(response.getMemberId())
                                                .build();
                                        return MemberUpdate.newBuilder()
                                                .setPrevHash(member.getLastHash())
                                                .setMemberId(member.getId())
                                                .addOperations(MemberOperation.newBuilder()
                                                        .setRecoveryRules(
                                                                MemberRecoveryRulesOperation
                                                                        .newBuilder()
                                                                        .setRecoveryRule(rule)))
                                                .build();
                                    }
                                });
                    }
                })
                .flatMapCompletable(new Function<MemberUpdate, Completable>() {
                    public Completable apply(MemberUpdate update) {
                        return toCompletable(gateway
                                .withAuthentication(authenticationContext())
                                .updateMember(UpdateMemberRequest.newBuilder()
                                        .setUpdate(update)
                                        .setUpdateSignature(Signature.newBuilder()
                                                .setKeyId(signer.getKeyId())
                                                .setMemberId(memberId)
                                                .setSignature(signer.sign(update)))
                                        .build()));
                    }
                });
    }

    /**
     * Gets all subscribers for the member.
     *
     * @return list of notification subscribers
     */
    public Observable<List<Subscriber>> getSubscribers() {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getSubscribers(GetSubscribersRequest
                        .newBuilder()
                        .build()))
                .map(new Function<GetSubscribersResponse, List<Subscriber>>() {
                    @Override
                    public List<Subscriber> apply(GetSubscribersResponse response) {
                        return response.getSubscribersList();
                    }
                });
    }

    /**
     * Gets a subscriber by Id.
     *
     * @param subscriberId subscriber id
     * @return notification subscriber
     */
    public Observable<Subscriber> getSubscriber(String subscriberId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getSubscriber(GetSubscriberRequest
                        .newBuilder()
                        .setSubscriberId(subscriberId)
                        .build()))
                .map(new Function<GetSubscriberResponse, Subscriber>() {
                    public Subscriber apply(GetSubscriberResponse response) {
                        return response.getSubscriber();
                    }
                });
    }

    /**
     * Removes a subscriber, to stop receiving notifications.
     *
     * @param subscriberId id of the subscriber
     * @return nothing
     */
    public Completable unsubscribeFromNotifications(String subscriberId) {
        return toCompletable(gateway
                .withAuthentication(authenticationContext())
                .unsubscribeFromNotifications(UnsubscribeFromNotificationsRequest
                        .newBuilder()
                        .setSubscriberId(subscriberId)
                        .build()));
    }

    /**
     * Gets a list of the member's notifications.
     *
     * @param offset offset to start
     * @param limit how many notifications to get
     * @return list of notifications
     */
    public Observable<PagedList<Notification, String>> getNotifications(
            @Nullable String offset,
            int limit) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getNotifications(GetNotificationsRequest
                        .newBuilder()
                        .setPage(pageBuilder(offset, limit))
                        .build()))
                .map(new Function<GetNotificationsResponse, PagedList<Notification, String>>() {
                    public PagedList<Notification, String> apply(
                            GetNotificationsResponse response) {
                        return PagedList.create(
                                response.getNotificationsList(),
                                response.getOffset());
                    }
                });
    }

    /**
     * Gets a notification.
     *
     * @param notificationId id of the notification
     * @return notification
     */
    public Observable<Notification> getNotification(String notificationId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getNotification(GetNotificationRequest
                        .newBuilder()
                        .setNotificationId(notificationId)
                        .build()))
                .map(new Function<GetNotificationResponse, Notification>() {
                    public Notification apply(GetNotificationResponse response) {
                        return response.getNotification();
                    }
                });
    }

    /**
     * Links a funding bank account to Token.
     *
     * @param authorization an authorization to accounts, from the bank
     * @return list of linked accounts
     */
    public Observable<List<Account>> linkAccounts(BankAuthorization authorization) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .linkAccounts(LinkAccountsRequest
                        .newBuilder()
                        .setBankAuthorization(authorization)
                        .build()))
                .map(new Function<LinkAccountsResponse, List<Account>>() {
                    public List<Account> apply(LinkAccountsResponse response) {
                        return response.getAccountsList();
                    }
                });
    }

    /**
     * Links a funding bank account to Token.
     *
     * @param authorization OAuth authorization for linking
     * @return list of linked accounts
     * @throws BankAuthorizationRequiredException if bank authorization payload
     *     is required to link accounts
     */
    public Observable<List<Account>> linkAccounts(OauthBankAuthorization authorization)
            throws BankAuthorizationRequiredException {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .linkAccountsOauth(LinkAccountsOauthRequest
                        .newBuilder()
                        .setAuthorization(authorization)
                        .build()))
                .map(new Function<LinkAccountsOauthResponse, List<Account>>() {
                    public List<Account> apply(LinkAccountsOauthResponse response) {
                        if (response.getStatus() == FAILURE_BANK_AUTHORIZATION_REQUIRED) {
                            throw new BankAuthorizationRequiredException();
                        }
                        return response.getAccountsList();
                    }
                });
    }

    /**
     * Unlinks token accounts.
     *
     * @param accountIds account ids to unlink
     * @return nothing
     */
    public Completable unlinkAccounts(List<String> accountIds) {
        return toCompletable(gateway
                .withAuthentication(authenticationContext())
                .unlinkAccounts(UnlinkAccountsRequest.newBuilder()
                        .addAllAccountIds(accountIds)
                        .build()));
    }

    /**
     * Looks up a linked funding account.
     *
     * @param accountId account id
     * @return account info
     */
    public Observable<Account> getAccount(String accountId) {
        return toObservable(gateway
                .withAuthentication(onBehalfOf())
                .getAccount(GetAccountRequest
                        .newBuilder()
                        .setAccountId(accountId)
                        .build()))
                .map(new Function<GetAccountResponse, Account>() {
                    public Account apply(GetAccountResponse response) {
                        return response.getAccount();
                    }
                });
    }

    /**
     * Looks up all the linked funding accounts.
     *
     * @return list of linked accounts
     */
    public Observable<List<Account>> getAccounts() {
        return toObservable(gateway
                .withAuthentication(onBehalfOf())
                .getAccounts(GetAccountsRequest
                        .newBuilder()
                        .build()))
                .map(new Function<GetAccountsResponse, List<Account>>() {
                    public List<Account> apply(GetAccountsResponse response) {
                        return response.getAccountsList();
                    }
                });
    }

    /**
     * Stores a token request.
     *
     * @param payload token payload
     * @param options map of options
     * @param userRefId (optional) user ref id
     * @param customizationId (optional) customization id
     * @return id to reference token request
     */
    @Deprecated
    public Observable<String> storeTokenRequest(
            TokenPayload payload,
            Map<String, String> options,
            @Nullable String userRefId,
            @Nullable String customizationId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .storeTokenRequest(StoreTokenRequestRequest.newBuilder()
                        .setPayload(payload)
                        .putAllOptions(options)
                        .setUserRefId(nullToEmpty(userRefId))
                        .setCustomizationId(nullToEmpty(customizationId))
                        .build()))
                .map(new Function<StoreTokenRequestResponse, String>() {
                    @Override
                    public String apply(StoreTokenRequestResponse storeTokenRequestResponse)
                            throws Exception {
                        return storeTokenRequestResponse.getTokenRequest().getId();
                    }
                });
    }

    /**
     * Stores a token request.
     *
     * @param payload token request payload
     * @param options token request options
     * @return reference id for token request
     */
    public Observable<String> storeTokenRequest(
            TokenRequestPayload payload,
            TokenRequestOptions options) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .storeTokenRequest(StoreTokenRequestRequest.newBuilder()
                        .setRequestPayload(payload)
                        .setRequestOptions(options)
                        .build()))
                .map(new Function<StoreTokenRequestResponse, String>() {
                    @Override
                    public String apply(StoreTokenRequestResponse storeTokenRequestResponse) {
                        return storeTokenRequestResponse.getTokenRequest().getId();
                    }
                });
    }

    /**
     * Updates an existing token request.
     *
     * @param requestId token request ID
     * @param options new token request options
     * @return completable
     */
    public Completable updateTokenRequest(String requestId, TokenRequestOptions options) {
        return toCompletable(gateway
                .withAuthentication(authenticationContext())
                .updateTokenRequest(UpdateTokenRequestRequest.newBuilder()
                        .setRequestId(requestId)
                        .setRequestOptions(options)
                        .build()));
    }

    /**
     * Creates a new transfer token.
     *
     * @param payload transfer token payload
     * @return transfer token returned by the server
     */
    public Observable<Token> createTransferToken(TokenPayload payload) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .createTransferToken(CreateTransferTokenRequest
                        .newBuilder()
                        .setPayload(payload)
                        .build()))
                .map(new Function<CreateTransferTokenResponse, Token>() {
                    public Token apply(CreateTransferTokenResponse response) {
                        if (response.getStatus() != TransferTokenStatus.SUCCESS) {
                            throw new TransferTokenException(response.getStatus());
                        }
                        return response.getToken();
                    }
                });
    }

    /**
     * Creates a new transfer token.
     *
     * @param payload transfer token payload
     * @param tokenRequestId token request id
     * @return transfer token returned by the server
     */
    public Observable<Token> createTransferToken(TokenPayload payload, String tokenRequestId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .createTransferToken(CreateTransferTokenRequest
                        .newBuilder()
                        .setPayload(payload)
                        .setTokenRequestId(tokenRequestId)
                        .build()))
                .map(new Function<CreateTransferTokenResponse, Token>() {
                    public Token apply(CreateTransferTokenResponse response) {
                        if (response.getStatus() != TransferTokenStatus.SUCCESS) {
                            throw new TransferTokenException(response.getStatus());
                        }
                        return response.getToken();
                    }
                });
    }

    /**
     * Creates a new access token.
     *
     * @param payload transfer token payload
     * @return access token returned by the server
     */
    public Observable<Token> createAccessToken(TokenPayload payload) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .createAccessToken(CreateAccessTokenRequest
                        .newBuilder()
                        .setPayload(payload)
                        .build()))
                .map(new Function<CreateAccessTokenResponse, Token>() {
                    public Token apply(CreateAccessTokenResponse response) {
                        return response.getToken();
                    }
                });
    }

    /**
     * Creates a new access token.
     *
     * @param tokenPayload token payload
     * @param tokenRequestId token request id
     * @return token returned by server
     */
    public Observable<Token> createAccessToken(TokenPayload tokenPayload, String tokenRequestId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .createAccessToken(CreateAccessTokenRequest.newBuilder()
                        .setPayload(tokenPayload)
                        .setTokenRequestId(tokenRequestId)
                        .build()))
                .map(new Function<CreateAccessTokenResponse, Token>() {
                    @Override
                    public Token apply(CreateAccessTokenResponse response)
                            throws Exception {
                        return response.getToken();
                    }
                });
    }

    /**
     * Looks up a existing token.
     *
     * @param tokenId token id
     * @return token returned by the server
     */
    public Observable<Token> getToken(String tokenId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getToken(GetTokenRequest
                        .newBuilder()
                        .setTokenId(tokenId)
                        .build()))
                .map(new Function<GetTokenResponse, Token>() {
                    public Token apply(GetTokenResponse response) {
                        return response.getToken();
                    }
                });
    }

    /**
     * Looks up a existing access token where the calling member is the grantor and given member is
     * the grantee.
     *
     * @param toMemberId beneficiary of the active access token
     * @return token returned by the server
     */
    public Observable<Token> getActiveAccessToken(String toMemberId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getActiveAccessToken(GetActiveAccessTokenRequest
                        .newBuilder()
                        .setToMemberId(toMemberId)
                        .build()))
                .map(new Function<GetActiveAccessTokenResponse, Token>() {
                    public Token apply(GetActiveAccessTokenResponse response) {
                        return response.getToken();
                    }
                });
    }

    /**
     * Looks up a list of existing token.
     *
     * @param type token type
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @return token returned by the server
     */
    public Observable<PagedList<Token, String>> getTokens(
            GetTokensRequest.Type type,
            @Nullable String offset,
            int limit) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getTokens(GetTokensRequest
                        .newBuilder()
                        .setType(type)
                        .setPage(pageBuilder(offset, limit))
                        .build()))
                .map(new Function<GetTokensResponse, PagedList<Token, String>>() {
                    public PagedList<Token, String> apply(GetTokensResponse response) {
                        return PagedList.create(response.getTokensList(), response.getOffset());
                    }
                });
    }

    /**
     * Endorses a token.
     *
     * @param token token to endorse
     * @param keyLevel key level to be used to endorse the token
     * @return result of the endorse operation, returned by the server
     */
    public Observable<TokenOperationResult> endorseToken(Token token, Key.Level keyLevel) {
        Signer signer = crypto.createSigner(keyLevel);
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .endorseToken(EndorseTokenRequest
                        .newBuilder()
                        .setTokenId(token.getId())
                        .setSignature(Signature
                                .newBuilder()
                                .setMemberId(memberId)
                                .setKeyId(signer.getKeyId())
                                .setSignature(signer.sign(tokenAction(token, ENDORSED))))
                        .build()))
                .map(new Function<EndorseTokenResponse, TokenOperationResult>() {
                    @Override
                    public TokenOperationResult apply(EndorseTokenResponse response) {
                        return response.getResult();
                    }
                });
    }

    /**
     * Cancels a token.
     *
     * @param token token to cancel
     * @return result of the cancel operation, returned by the server
     */
    public Observable<TokenOperationResult> cancelToken(Token token) {
        Signer signer = crypto.createSigner(Key.Level.LOW);
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .cancelToken(CancelTokenRequest
                        .newBuilder()
                        .setTokenId(token.getId())
                        .setSignature(Signature
                                .newBuilder()
                                .setMemberId(memberId)
                                .setKeyId(signer.getKeyId())
                                .setSignature(signer.sign(tokenAction(token, CANCELLED))))
                        .build()))
                .map(new Function<CancelTokenResponse, TokenOperationResult>() {
                    public TokenOperationResult apply(CancelTokenResponse response) {
                        return response.getResult();
                    }
                });
    }

    /**
     * Makes RPC to get default bank account for this member.
     *
     * @param memberId the member id
     * @return the bank account
     */
    public Observable<Account> getDefaultAccount(String memberId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getDefaultAccount(GetDefaultAccountRequest.newBuilder()
                        .setMemberId(memberId)
                        .build()))
                .map(new Function<GetDefaultAccountResponse, Account>() {
                    public Account apply(GetDefaultAccountResponse response) {
                        return response.getAccount();
                    }
                });
    }

    /**
     * Makes RPC to set default bank account.
     *
     * @param accountId the bank account id
     * @return completable indicating if the default bank account was successfully set
     */
    public Completable setDefaultAccount(String accountId) {
        return toCompletable(gateway
                .withAuthentication(authenticationContext())
                .setDefaultAccount(SetDefaultAccountRequest
                        .newBuilder()
                        .setMemberId(memberId)
                        .setAccountId(accountId)
                        .build()));
    }

    /**
     * Looks up if this account is default.
     *
     * @param accountId the bank account id
     * @return true if the account is default; false otherwise
     */
    public Observable<Boolean> isDefault(final String accountId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getDefaultAccount(GetDefaultAccountRequest.newBuilder()
                        .setMemberId(memberId)
                        .build()))
                .map(new Function<GetDefaultAccountResponse, Boolean>() {
                    public Boolean apply(GetDefaultAccountResponse response) {
                        return response.getAccount().getId().contentEquals(accountId);
                    }
                });
    }

    /**
     * Cancels the existing token and creates a replacement for it.
     * Supported only for access tokens.
     *
     * @param tokenToCancel old token to cancel
     * @param tokenToCreate new token to create
     * @return result of the replacement operation, returned by the server
     */
    public Observable<TokenOperationResult> replace(
            Token tokenToCancel,
            TokenPayload tokenToCreate) {
        return cancelAndReplace(tokenToCancel, CreateToken.newBuilder().setPayload(tokenToCreate));
    }

    /**
     * Cancels the existing token, creates a replacement and endorses it.
     * Supported only for access tokens.
     *
     * @param tokenToCancel old token to cancel
     * @param tokenToCreate new token to create
     * @deprecated use {@link #replace} and {@link #endorseToken} instead
     * @return result of the replacement operation, returned by the server
     */
    @Deprecated
    public Observable<TokenOperationResult> replaceAndEndorseToken(
            Token tokenToCancel,
            TokenPayload tokenToCreate) {
        Signer signer = crypto.createSigner(STANDARD);
        CreateToken.Builder createToken = CreateToken.newBuilder().setPayload(tokenToCreate);
        createToken.setPayloadSignature(Signature.newBuilder()
                .setMemberId(memberId)
                .setKeyId(signer.getKeyId())
                .setSignature(signer.sign(tokenAction(tokenToCreate, ENDORSED))));
        return cancelAndReplace(tokenToCancel, createToken);
    }


    /**
     * Look up account balance.
     *
     * @param accountId account id
     * @param keyLevel key level
     * @return account balance
     */
    public Observable<Balance> getBalance(String accountId, Key.Level keyLevel) {
        return toObservable(gateway
                .withAuthentication(onBehalfOf(keyLevel))
                .getBalance(GetBalanceRequest.newBuilder()
                        .setAccountId(accountId)
                        .build()))
                .map(new Function<GetBalanceResponse, Balance>() {
                    public Balance apply(GetBalanceResponse response) {
                        if (response.getStatus() == SUCCESSFUL_REQUEST) {
                            return response.getBalance();
                        } else {
                            throw new StepUpRequiredException("Balance step up required.");
                        }
                    }
                });
    }

    /**
     * Look up balances for a list of accounts.
     *
     * @param accountIds list of account ids
     * @param keyLevel key level
     * @return list of balances
     */
    public Observable<List<Balance>> getBalances(List<String> accountIds, Key.Level keyLevel) {
        return toObservable(gateway
                .withAuthentication(onBehalfOf(keyLevel))
                .getBalances(GetBalancesRequest
                        .newBuilder()
                        .addAllAccountId(accountIds)
                        .build()))
                .map(new Function<GetBalancesResponse, List<Balance>>() {
                    public List<Balance> apply(GetBalancesResponse response) {
                        List<Balance> balances = new ArrayList<>();
                        for (GetBalanceResponse getBalanceResponse : response.getResponseList()) {
                            if (getBalanceResponse.getStatus() == SUCCESSFUL_REQUEST) {
                                balances.add(getBalanceResponse.getBalance());
                            }
                        }
                        return balances;
                    }
                });
    }

    /**
     * Redeems a transfer token.
     *
     * @param transfer transfer parameters, such as amount, currency, etc
     * @return transfer record
     */
    public Observable<Transfer> createTransfer(TransferPayload transfer) {
        Signer signer = crypto.createSigner(Key.Level.LOW);
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .createTransfer(CreateTransferRequest
                        .newBuilder()
                        .setPayload(transfer)
                        .setPayloadSignature(Signature
                                .newBuilder()
                                .setMemberId(memberId)
                                .setKeyId(signer.getKeyId())
                                .setSignature(signer.sign(transfer)))
                        .build()))
                .map(new Function<CreateTransferResponse, Transfer>() {
                    public Transfer apply(CreateTransferResponse response) {
                        return response.getTransfer();
                    }
                });
    }

    /**
     * Looks up an existing transfer.
     *
     * @param transferId transfer id
     * @return transfer record
     */
    public Observable<Transfer> getTransfer(String transferId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getTransfer(GetTransferRequest
                        .newBuilder()
                        .setTransferId(transferId)
                        .build()))
                .map(new Function<GetTransferResponse, Transfer>() {
                    public Transfer apply(GetTransferResponse response) {
                        return response.getTransfer();
                    }
                });
    }

    /**
     * Looks up a list of existing transfers.
     *
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @param tokenId optional token id to restrict the search
     * @return transfer record
     */
    public Observable<PagedList<Transfer, String>> getTransfers(
            @Nullable String offset,
            int limit,
            @Nullable String tokenId) {
        GetTransfersRequest.Builder request = GetTransfersRequest
                .newBuilder()
                .setPage(pageBuilder(offset, limit));

        if (tokenId != null) {
            request.setFilter(GetTransfersRequest
                    .TransferFilter
                    .newBuilder()
                    .setTokenId(tokenId)
                    .build());
        }

        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getTransfers(request.build()))
                .map(new Function<GetTransfersResponse, PagedList<Transfer, String>>() {
                    public PagedList<Transfer, String> apply(GetTransfersResponse response) {
                        return PagedList.create(response.getTransfersList(), response.getOffset());
                    }
                });
    }

    /**
     * Look up an existing transaction and return the response.
     *
     * @param accountId account id
     * @param transactionId transaction id
     * @param keyLevel key level
     * @return transaction
     */
    public Observable<Transaction> getTransaction(
            String accountId,
            String transactionId,
            Key.Level keyLevel) {
        return toObservable(gateway
                .withAuthentication(onBehalfOf(keyLevel))
                .getTransaction(GetTransactionRequest
                        .newBuilder()
                        .setAccountId(accountId)
                        .setTransactionId(transactionId)
                        .build()))
                .map(new Function<GetTransactionResponse, Transaction>() {
                    public Transaction apply(GetTransactionResponse response) {
                        if (response.getStatus() == SUCCESSFUL_REQUEST) {
                            return response.getTransaction();
                        } else {
                            throw new StepUpRequiredException("Transaction step up required.");
                        }
                    }
                });
    }

    /**
     * Lookup transactions and return response.
     *
     * @param accountId account id
     * @param offset offset
     * @param limit limit
     * @param keyLevel key level
     * @return paged list of transactions
     */
    public Observable<PagedList<Transaction, String>> getTransactions(
            String accountId,
            @Nullable String offset,
            int limit,
            Key.Level keyLevel) {
        return toObservable(gateway
                .withAuthentication(onBehalfOf(keyLevel))
                .getTransactions(GetTransactionsRequest
                        .newBuilder()
                        .setAccountId(accountId)
                        .setPage(pageBuilder(offset, limit))
                        .build()))
                .map(new Function<GetTransactionsResponse, PagedList<Transaction, String>>() {
                    public PagedList<Transaction, String> apply(GetTransactionsResponse response) {
                        if (response.getStatus() == SUCCESSFUL_REQUEST) {
                            return PagedList.create(
                                    response.getTransactionsList(),
                                    response.getOffset());
                        } else {
                            throw new StepUpRequiredException("Transactions step up required.");
                        }
                    }
                });
    }

    /**
     * Creates and uploads a blob.
     *
     * @param payload payload of the blob
     * @return id of the blob
     */
    public Observable<String> createBlob(Payload payload) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .createBlob(CreateBlobRequest
                        .newBuilder()
                        .setPayload(payload)
                        .build()))
                .map(new Function<CreateBlobResponse, String>() {
                    public String apply(CreateBlobResponse response) {
                        return response.getBlobId();
                    }
                });
    }

    /**
     * Retrieves a blob from the server.
     *
     * @param blobId id of the blob
     * @return Blob
     */
    public Observable<Blob> getBlob(String blobId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getBlob(GetBlobRequest
                        .newBuilder()
                        .setBlobId(blobId)
                        .build()))
                .map(new Function<GetBlobResponse, Blob>() {
                    public Blob apply(GetBlobResponse response) {
                        return response.getBlob();
                    }
                });
    }

    /**
     * Retrieves a blob that is attached to a token.
     *
     * @param tokenId id of the token
     * @param blobId id of the blob
     * @return Blob
     */
    public Observable<Blob> getTokenBlob(String tokenId, String blobId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getTokenBlob(GetTokenBlobRequest
                        .newBuilder()
                        .setTokenId(tokenId)
                        .setBlobId(blobId)
                        .build()))
                .map(new Function<GetTokenBlobResponse, Blob>() {
                    public Blob apply(GetTokenBlobResponse response) {
                        return response.getBlob();
                    }
                });
    }

    /**
     * Adds a new member address.
     *
     * @param name the name of the address
     * @param address the address json
     * @return an address record created
     */
    public Observable<AddressRecord> addAddress(String name, Address address) {
        Signer signer = crypto.createSigner(Key.Level.LOW);
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .addAddress(AddAddressRequest
                        .newBuilder()
                        .setName(name)
                        .setAddress(address)
                        .setAddressSignature(Signature
                                .newBuilder()
                                .setMemberId(memberId)
                                .setKeyId(signer.getKeyId())
                                .setSignature(signer.sign(address))
                                .build())
                        .build()))
                .map(new Function<AddAddressResponse, AddressRecord>() {
                    public AddressRecord apply(AddAddressResponse response) {
                        return response.getAddress();
                    }
                });
    }

    /**
     * Looks up an address by id.
     *
     * @param addressId the address id
     * @return an address record
     */
    public Observable<AddressRecord> getAddress(String addressId) {
        return toObservable(gateway
                .withAuthentication(onBehalfOf())
                .getAddress(GetAddressRequest
                        .newBuilder()
                        .setAddressId(addressId)
                        .build()))
                .map(new Function<GetAddressResponse, AddressRecord>() {
                    public AddressRecord apply(GetAddressResponse response) {
                        return response.getAddress();
                    }
                });
    }

    /**
     * Looks up member addresses.
     *
     * @return a list of addresses
     */
    public Observable<List<AddressRecord>> getAddresses() {
        return toObservable(gateway
                .withAuthentication(onBehalfOf())
                .getAddresses(GetAddressesRequest
                        .newBuilder()
                        .build()))
                .map(new Function<GetAddressesResponse, List<AddressRecord>>() {
                    public List<AddressRecord> apply(GetAddressesResponse response) {
                        return response.getAddressesList();
                    }
                });
    }

    /**
     * Deletes a member address by its id.
     *
     * @param addressId the id of the address
     * @return observable that completes when request
     */
    public Completable deleteAddress(String addressId) {
        return toCompletable(gateway
                .withAuthentication(authenticationContext())
                .deleteAddress(DeleteAddressRequest
                        .newBuilder()
                        .setAddressId(addressId)
                        .build()));
    }

    /**
     * Replaces a member's public profile.
     *
     * @param profile Profile to set
     * @return observable that completes when request handled
     */
    public Observable<Profile> setProfile(Profile profile) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .setProfile(SetProfileRequest.newBuilder()
                        .setProfile(profile)
                        .build()))
                .map(new Function<SetProfileResponse, Profile>() {
                    public Profile apply(SetProfileResponse response) {
                        return response.getProfile();
                    }
                });
    }

    /**
     * Gets a member's public profile.
     *
     * @param memberId member Id whose profile we want
     * @return their profile text
     */
    public Observable<Profile> getProfile(String memberId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getProfile(GetProfileRequest.newBuilder()
                        .setMemberId(memberId)
                        .build()))
                .map(new Function<GetProfileResponse, Profile>() {
                    public Profile apply(GetProfileResponse response) {
                        return response.getProfile();
                    }
                });
    }

    /**
     * Replaces a member's public profile picture.
     *
     * @param payload Picture data
     * @return observable that completes when request handled
     */
    public Completable setProfilePicture(Payload payload) {
        return toCompletable(gateway
                .withAuthentication(authenticationContext())
                .setProfilePicture(SetProfilePictureRequest.newBuilder()
                        .setPayload(payload)
                        .build()));
    }

    /**
     * Gets a member's public profile picture.
     *
     * @param memberId member Id whose profile we want
     * @param size size category we want (small, medium, large, original)
     * @return blob with picture; empty blob (no fields set) if has no picture
     */
    public Observable<Blob> getProfilePicture(String memberId, ProfilePictureSize size) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getProfilePicture(GetProfilePictureRequest.newBuilder()
                        .setMemberId(memberId)
                        .setSize(size)
                        .build()))
                .map(new Function<GetProfilePictureResponse, Blob>() {
                    public Blob apply(GetProfilePictureResponse response) {
                        return response.getBlob();
                    }
                });
    }

    /**
     * Replaces member's receipt contact.
     *
     * @param contact receipt contact to set
     * @return completable that indicates whether the operation finished or had an error
     */
    public Completable setReceiptContact(ReceiptContact contact) {
        return toCompletable(gateway
                .withAuthentication(authenticationContext())
                .setReceiptContact(SetReceiptContactRequest.newBuilder()
                        .setContact(contact)
                        .build()));
    }

    /**
     * Gets a member's receipt contact.
     *
     * @return receipt contact
     */
    public Observable<ReceiptContact> getReceiptContact() {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getReceiptContact(GetReceiptContactRequest.getDefaultInstance()))
                .map(new Function<GetReceiptContactResponse, ReceiptContact>() {
                    @Override
                    public ReceiptContact apply(
                            GetReceiptContactResponse getReceiptContactResponse) {
                        return getReceiptContactResponse.getContact();
                    }
                });
    }

    /**
     * Returns linking information for the specified bank id.
     *
     * @param bankId the bank id
     * @return bank linking information
     */
    public Observable<BankInfo> getBankInfo(String bankId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getBankInfo(GetBankInfoRequest
                        .newBuilder()
                        .setBankId(bankId)
                        .build()))
                .map(new Function<GetBankInfoResponse, BankInfo>() {
                    public BankInfo apply(GetBankInfoResponse response) {
                        return response.getInfo();
                    }
                });
    }

    /**
     * Creates a test bank account and links it.
     *
     * @param balance account balance to set
     * @return linked account
     */
    public Observable<Account> createAndLinkTestBankAccount(Money balance) {
        return createTestBankAccount(balance)
                .flatMap(new Function<OauthBankAuthorization, Observable<Account>>() {
                    @Override
                    public Observable<Account> apply(OauthBankAuthorization authorization) {
                        return linkAccounts(authorization)
                                .map(new Function<List<Account>, Account>() {
                                    @Override
                                    public Account apply(List<Account> accounts) {
                                        if (accounts.size() != 1) {
                                            throw new RuntimeException(
                                                    "Expected 1 account; found "
                                                            + accounts.size());
                                        }
                                        return accounts.get(0);
                                    }
                                });
                    }
                });
    }

    /**
     * Creates a test bank account and returns the authorization for it.
     *
     * @param balance account balance to set
     * @return OAuth bank authorization
     */
    public Observable<OauthBankAuthorization> createTestBankAccount(Money balance) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .createTestBankAccount(CreateTestBankAccountRequest.newBuilder()
                        .setBalance(balance)
                        .build()))
                .map(new Function<CreateTestBankAccountResponse, OauthBankAuthorization>() {
                    public OauthBankAuthorization apply(CreateTestBankAccountResponse response) {
                        return response.getAuthorization();
                    }
                });
    }

    /**
     * Returns a list of aliases of the member.
     *
     * @return a list of aliases
     */
    public Observable<List<Alias>> getAliases() {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getAliases(GetAliasesRequest
                        .newBuilder()
                        .build()))
                .map(new Function<GetAliasesResponse, List<Alias>>() {
                    public List<Alias> apply(GetAliasesResponse response) {
                        return response.getAliasesList();
                    }
                });
    }

    /**
     * Retry alias verification.
     *
     * @param alias the alias to be verified
     * @return the verification id
     */
    public Observable<String> retryVerification(Alias alias) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .retryVerification(RetryVerificationRequest.newBuilder()
                        .setAlias(alias)
                        .setMemberId(memberId)
                        .build()))
                .map(new Function<RetryVerificationResponse, String>() {
                    public String apply(RetryVerificationResponse response) {
                        return response.getVerificationId();
                    }
                });
    }

    /**
     * Authorizes recovery as a trusted agent.
     *
     * @param authorization the authorization
     * @return the signature
     */
    public Observable<Signature> authorizeRecovery(Authorization authorization) {
        Signer signer = crypto.createSigner(PRIVILEGED);
        return Observable.just(Signature.newBuilder()
                .setMemberId(memberId)
                .setKeyId(signer.getKeyId())
                .setSignature(signer.sign(authorization))
                .build());
    }

    /**
     * Gets the member id of the default recovery agent.
     *
     * @return the member id
     */
    public Observable<String> getDefaultAgent() {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getDefaultAgent(GetDefaultAgentRequest.getDefaultInstance()))
                .map(new Function<GetDefaultAgentResponse, String>() {
                    public String apply(GetDefaultAgentResponse response) {
                        return response.getMemberId();
                    }
                });
    }

    /**
     * Verifies a given alias.
     *
     * @param verificationId the verification id
     * @param code the code
     * @return a completable
     */
    public Completable verifyAlias(String verificationId, String code) {
        return toCompletable(gateway
                .withAuthentication(authenticationContext())
                .verifyAlias(VerifyAliasRequest.newBuilder()
                        .setVerificationId(verificationId)
                        .setCode(code)
                        .build()));
    }

    /**
     * Trigger a step up notification for tokens.
     *
     * @param tokenId token id
     * @return notification status
     */
    public Observable<NotifyStatus> triggerTokenStepUpNotification(String tokenId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .triggerStepUpNotification(TriggerStepUpNotificationRequest.newBuilder()
                        .setTokenStepUp(StepUp.newBuilder()
                                .setTokenId(tokenId))
                        .build()))
                .map(new Function<TriggerStepUpNotificationResponse, NotifyStatus>() {
                    public NotifyStatus apply(TriggerStepUpNotificationResponse response) {
                        return response.getStatus();
                    }
                });
    }

    /**
     * Trigger a step up notification for balance requests.
     *
     * @param accountIds list of account ids
     * @return notification status
     */
    public Observable<NotifyStatus> triggerBalanceStepUpNotification(List<String> accountIds) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .triggerStepUpNotification(TriggerStepUpNotificationRequest.newBuilder()
                        .setBalanceStepUp(NotificationProtos.BalanceStepUp.newBuilder()
                                .addAllAccountId(accountIds))
                        .build()))
                .map(new Function<TriggerStepUpNotificationResponse, NotifyStatus>() {
                    public NotifyStatus apply(TriggerStepUpNotificationResponse response) {
                        return response.getStatus();
                    }
                });
    }

    /**
     * Trigger a step up notification for transaction requests.
     *
     * @param accountId account id
     * @return notification status
     */
    public Observable<NotifyStatus> triggerTransactionStepUpNotification(String accountId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .triggerStepUpNotification(TriggerStepUpNotificationRequest
                        .newBuilder()
                        .setTransactionStepUp(NotificationProtos.TransactionStepUp.newBuilder()
                                .setAccountId(accountId))
                        .build()))
                .map(new Function<TriggerStepUpNotificationResponse, NotifyStatus>() {
                    public NotifyStatus apply(TriggerStepUpNotificationResponse response) {
                        return response.getStatus();
                    }
                });
    }

    /**
     * Apply SCA for the given list of account IDs.
     *
     * @param accountIds list of account ids
     * @return completable
     */
    public Completable applySca(List<String> accountIds) {
        return toCompletable(gateway
                .withAuthentication(authenticationContext(STANDARD))
                .applySca(ApplyScaRequest.newBuilder()
                        .addAllAccountId(accountIds)
                        .build()));
    }

    /**
     * Sign with a Token signature a token request state payload.
     *
     * @param tokenRequestId token request id
     * @param tokenId token id
     * @param state state
     * @return signature
     */
    public Observable<Signature> signTokenRequestState(
            String tokenRequestId,
            String tokenId,
            String state) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .signTokenRequestState(SignTokenRequestStateRequest.newBuilder()
                .setPayload(TokenRequestStatePayload.newBuilder()
                        .setTokenId(tokenId)
                        .setState(state))
                .setTokenRequestId(tokenRequestId)
                .build()))
                .map(new Function<SignTokenRequestStateResponse, Signature>() {
                    public Signature apply(SignTokenRequestStateResponse response) {
                        return response.getSignature();
                    }
                });
    }

    /**
     * Get list of paired devices.
     *
     * @return list of devices
     */
    public Observable<List<Device>> getPairedDevices() {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getPairedDevices(GetPairedDevicesRequest.getDefaultInstance()))
                .map(new Function<GetPairedDevicesResponse, List<Device>>() {
                    @Override
                    public List<Device> apply(GetPairedDevicesResponse response)
                            throws Exception {
                        return response.getDevicesList();
                    }
                });
    }

    /**
     * Delete the member.
     *
     * @return completable
     */
    public Completable deleteMember() {
        return toCompletable(gateway
                .withAuthentication(authenticationContext(PRIVILEGED))
                .deleteMember(DeleteMemberRequest.getDefaultInstance()));
    }

    /**
     * Verifies an affiliated TPP.
     *
     * @param memberId member ID of the TPP to verify
     * @return completable
     */
    public Completable verifyAffiliate(String memberId) {
        return toCompletable(gateway
                .withAuthentication(authenticationContext())
                .verifyAffiliate(VerifyAffiliateRequest.newBuilder()
                        .setMemberId(memberId)
                        .build()));
    }

    /**
     * Resolves transfer destinations for the given account ID.
     *
     * @param accountId account ID
     * @return transfer endpoints
     */
    public Observable<List<TransferEndpoint>> resolveTransferDestinations(String accountId) {
        return toObservable(gateway
                .withAuthentication(onBehalfOf())
                .resolveTransferDestinations(ResolveTransferDestinationsRequest.newBuilder()
                        .setAccountId(accountId)
                        .build()))
                .map(new Function<ResolveTransferDestinationsResponse, List<TransferEndpoint>>() {
                    @Override
                    public List<TransferEndpoint> apply(
                            ResolveTransferDestinationsResponse response) {
                        return response.getDestinationsList();
                    }
                });
    }

    /**
     * Adds a trusted beneficiary for whom the SCA will be skipped.
     *
     * @param payload the payload of the request
     * @return a completable
     */
    public Completable addTrustedBeneficiary(TrustedBeneficiary.Payload payload) {
        Signer signer = crypto.createSigner(STANDARD);
        return toCompletable(gateway
                .withAuthentication(authenticationContext())
                .addTrustedBeneficiary(AddTrustedBeneficiaryRequest.newBuilder()
                        .setTrustedBeneficiary(TrustedBeneficiary.newBuilder()
                                .setPayload(payload)
                                .setSignature(Signature.newBuilder()
                                        .setKeyId(signer.getKeyId())
                                        .setMemberId(memberId)
                                        .setSignature(signer.sign(payload))))
                        .build()));
    }

    /**
     * Removes a trusted beneficiary.
     *
     * @param payload the payload of the request
     * @return a completable
     */
    public Completable removeTrustedBeneficiary(TrustedBeneficiary.Payload payload) {
        Signer signer = crypto.createSigner(STANDARD);
        return toCompletable(gateway
                .withAuthentication(authenticationContext())
                .removeTrustedBeneficiary(RemoveTrustedBeneficiaryRequest.newBuilder()
                        .setTrustedBeneficiary(TrustedBeneficiary.newBuilder()
                                .setPayload(payload)
                                .setSignature(Signature.newBuilder()
                                        .setKeyId(signer.getKeyId())
                                        .setMemberId(memberId)
                                        .setSignature(signer.sign(payload))))
                        .build()));
    }

    /**
     * Gets a list of all trusted beneficiaries.
     *
     * @return the list
     */
    public Observable<List<TrustedBeneficiary>> getTrustedBeneficiaries() {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getTrustedBeneficiaries(GetTrustedBeneficiariesRequest.getDefaultInstance()))
                .map(new Function<GetTrustedBeneficiariesResponse, List<TrustedBeneficiary>>() {
                    @Override
                    public List<TrustedBeneficiary> apply(GetTrustedBeneficiariesResponse res) {
                        return res.getTrustedBeneficiariesList();
                    }
                });
    }

    /**
     * Creates a customization.
     *
     * @param logo logo
     * @param colors map of ARGB colors #AARRGGBB
     * @param consentText consent text
     * @return customization id
     */
    public Observable<String> createCustomization(
            Payload logo,
            Map<String, String> colors,
            String consentText) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .createCustomization(CreateCustomizationRequest.newBuilder()
                        .setLogo(logo)
                        .putAllColors(colors)
                        .setConsentText(consentText)
                        .build()))
                .map(new Function<CreateCustomizationResponse, String>() {
                    @Override
                    public String apply(CreateCustomizationResponse createCustomizationResponse) {
                        return createCustomizationResponse.getCustomizationId();
                    }
                });
    }

    private Observable<TokenOperationResult> cancelAndReplace(
            Token tokenToCancel,
            CreateToken.Builder createToken) {
        Signer signer = crypto.createSigner(Key.Level.LOW);
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .replaceToken(ReplaceTokenRequest
                        .newBuilder()
                        .setCancelToken(CancelToken
                                .newBuilder()
                                .setTokenId(tokenToCancel.getId())
                                .setSignature(Signature
                                        .newBuilder()
                                        .setMemberId(memberId)
                                        .setKeyId(signer.getKeyId())
                                        .setSignature(signer
                                                .sign(tokenAction(tokenToCancel, CANCELLED)))))
                        .setCreateToken(createToken)
                        .build()))
                .map(new Function<ReplaceTokenResponse, TokenOperationResult>() {
                    public TokenOperationResult apply(ReplaceTokenResponse response) {
                        return response.getResult();
                    }
                });
    }

    public CryptoEngine getCryptoEngine() {
        return crypto;
    }

    /**
     * Sets security metadata included in all requests.
     *
     * @param securityMetadata security metadata
     */
    public void setSecurityMetadata(SecurityMetadata securityMetadata) {
        this.securityMetadata = securityMetadata;
    }

    /**
     * Clears security metadata.
     */
    public void clearSecurityMetadata() {
        this.securityMetadata = SecurityMetadata.getDefaultInstance();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Client)) {
            return false;
        }

        Client other = (Client) obj;
        return memberId.equals(other.memberId)
                && Util.compare(onBehalfOf, other.onBehalfOf);
    }

    @Override
    public int hashCode() {
        return (memberId + (onBehalfOf == null ? "" : onBehalfOf)).hashCode();
    }

    private AuthenticationContext authenticationContext() {
        return AuthenticationContext.create(null, false, LOW, securityMetadata);
    }

    private AuthenticationContext authenticationContext(Key.Level level) {
        return AuthenticationContext.create(null, false, level, securityMetadata);
    }

    private AuthenticationContext onBehalfOf() {
        return AuthenticationContext.create(onBehalfOf, customerInitiated, LOW, securityMetadata);
    }

    private AuthenticationContext onBehalfOf(Key.Level level) {
        return AuthenticationContext.create(onBehalfOf, customerInitiated, level, securityMetadata);
    }

    private Page.Builder pageBuilder(@Nullable String offset, int limit) {
        Page.Builder page = Page.newBuilder()
                .setLimit(limit);
        if (offset != null) {
            page.setOffset(offset);
        }

        return page;
    }

    private String tokenAction(Token token, Action action) {
        return tokenAction(token.getPayload(), action);
    }

    private String tokenAction(TokenPayload tokenPayload, Action action) {
        return String.format(
                "%s.%s",
                toJson(tokenPayload),
                action.name().toLowerCase());
    }

    interface GatewayProvider {
        GatewayServiceFutureStub withAuthentication(AuthenticationContext context);
    }
}
