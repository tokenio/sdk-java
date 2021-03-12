/**
 * Copyright (c) 2021 Token, Inc.
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

package io.token.user.rpc;

import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.proto.common.token.TokenProtos.TokenSignature.Action.CANCELLED;
import static io.token.proto.common.token.TokenProtos.TokenSignature.Action.ENDORSED;
import static io.token.rpc.util.Converters.toCompletable;
import static io.token.util.Util.toObservable;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.token.proto.PagedList;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.proto.common.account.AccountProtos.Account;
import io.token.proto.common.blob.BlobProtos.Blob;
import io.token.proto.common.member.MemberProtos.Profile;
import io.token.proto.common.member.MemberProtos.ReceiptContact;
import io.token.proto.common.notification.NotificationProtos.Notification;
import io.token.proto.common.notification.NotificationProtos.Notification.Status;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.security.SecurityProtos.Signature;
import io.token.proto.common.submission.SubmissionProtos.StandingOrderSubmission;
import io.token.proto.common.subscriber.SubscriberProtos.Subscriber;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.proto.common.token.TokenProtos.TokenRequestStatePayload;
import io.token.proto.common.token.TokenProtos.TransferTokenStatus;
import io.token.proto.common.transfer.TransferProtos.BulkTransfer;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.proto.common.transfer.TransferProtos.TransferPayload;
import io.token.proto.gateway.Gateway;
import io.token.proto.gateway.Gateway.ApplyScaRequest;
import io.token.proto.gateway.Gateway.CancelTokenRequest;
import io.token.proto.gateway.Gateway.CancelTokenResponse;
import io.token.proto.gateway.Gateway.CreateAccessTokenRequest;
import io.token.proto.gateway.Gateway.CreateAccessTokenResponse;
import io.token.proto.gateway.Gateway.CreateBulkTransferRequest;
import io.token.proto.gateway.Gateway.CreateBulkTransferResponse;
import io.token.proto.gateway.Gateway.CreateStandingOrderRequest;
import io.token.proto.gateway.Gateway.CreateStandingOrderResponse;
import io.token.proto.gateway.Gateway.CreateTokenRequest;
import io.token.proto.gateway.Gateway.CreateTokenResponse;
import io.token.proto.gateway.Gateway.CreateTransferRequest;
import io.token.proto.gateway.Gateway.CreateTransferResponse;
import io.token.proto.gateway.Gateway.CreateTransferTokenRequest;
import io.token.proto.gateway.Gateway.EndorseTokenRequest;
import io.token.proto.gateway.Gateway.EndorseTokenResponse;
import io.token.proto.gateway.Gateway.GetActiveAccessTokenRequest;
import io.token.proto.gateway.Gateway.GetActiveAccessTokenResponse;
import io.token.proto.gateway.Gateway.GetBulkTransferRequest;
import io.token.proto.gateway.Gateway.GetBulkTransferResponse;
import io.token.proto.gateway.Gateway.GetDefaultAccountRequest;
import io.token.proto.gateway.Gateway.GetDefaultAccountResponse;
import io.token.proto.gateway.Gateway.GetNotificationRequest;
import io.token.proto.gateway.Gateway.GetNotificationResponse;
import io.token.proto.gateway.Gateway.GetNotificationsRequest;
import io.token.proto.gateway.Gateway.GetReceiptContactRequest;
import io.token.proto.gateway.Gateway.GetReceiptContactResponse;
import io.token.proto.gateway.Gateway.GetStandingOrderSubmissionResponse;
import io.token.proto.gateway.Gateway.GetStandingOrderSubmissionsRequest;
import io.token.proto.gateway.Gateway.GetSubscriberRequest;
import io.token.proto.gateway.Gateway.GetSubscriberResponse;
import io.token.proto.gateway.Gateway.GetSubscribersRequest;
import io.token.proto.gateway.Gateway.GetSubscribersResponse;
import io.token.proto.gateway.Gateway.GetTokenRequest;
import io.token.proto.gateway.Gateway.GetTokenResponse;
import io.token.proto.gateway.Gateway.GetTokensRequest;
import io.token.proto.gateway.Gateway.GetTransferRequest;
import io.token.proto.gateway.Gateway.GetTransferResponse;
import io.token.proto.gateway.Gateway.GetTransfersRequest;
import io.token.proto.gateway.Gateway.LinkAccountsRequest;
import io.token.proto.gateway.Gateway.LinkAccountsResponse;
import io.token.proto.gateway.Gateway.PrepareTokenRequest;
import io.token.proto.gateway.Gateway.ReplaceTokenRequest;
import io.token.proto.gateway.Gateway.ReplaceTokenRequest.CancelToken;
import io.token.proto.gateway.Gateway.ReplaceTokenRequest.CreateToken;
import io.token.proto.gateway.Gateway.ReplaceTokenResponse;
import io.token.proto.gateway.Gateway.SetDefaultAccountRequest;
import io.token.proto.gateway.Gateway.SetProfilePictureRequest;
import io.token.proto.gateway.Gateway.SetProfileRequest;
import io.token.proto.gateway.Gateway.SetProfileResponse;
import io.token.proto.gateway.Gateway.SetReceiptContactRequest;
import io.token.proto.gateway.Gateway.SignTokenRequestStateRequest;
import io.token.proto.gateway.Gateway.SignTokenRequestStateResponse;
import io.token.proto.gateway.Gateway.StoreLinkingRequestRequest;
import io.token.proto.gateway.Gateway.StoreLinkingRequestResponse;
import io.token.proto.gateway.Gateway.SubscribeToNotificationsRequest;
import io.token.proto.gateway.Gateway.SubscribeToNotificationsResponse;
import io.token.proto.gateway.Gateway.UnlinkAccountsRequest;
import io.token.proto.gateway.Gateway.UnsubscribeFromNotificationsRequest;
import io.token.proto.gateway.Gateway.UpdateNotificationStatusRequest;
import io.token.rpc.GatewayProvider;
import io.token.security.CryptoEngine;
import io.token.security.Signer;
import io.token.user.PrepareTokenResult;
import io.token.user.exceptions.TransferTokenException;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * An authenticated RPC client that is used to talk to Token gateway. The
 * class is a thin wrapper on top of gRPC generated client. Makes the API
 * easier to use.
 */
public final class Client extends io.token.rpc.Client {
    /**
     * Creates a client instance.
     *
     * @param memberId member id
     * @param crypto the crypto engine used to sign for authentication, request payloads, etc
     * @param gateway gateway gRPC stub
     */
    Client(
            String memberId,
            CryptoEngine crypto,
            GatewayProvider gateway) {
        super(memberId, crypto, gateway);
    }

    /**
     * Replaces a member's profile name.
     *
     * @param profileName profile name to set
     * @return completable that completes when request handled
     */
    public Completable setProfileName(String profileName) {
        return toCompletable(gateway
                .withAuthentication(authenticationContext())
                .setProfileName(Gateway.SetProfileNameRequest.newBuilder()
                        .setProfileName(profileName)
                        .build()));
    }

    /**
     * Replaces a member's public profile picture.
     *
     * @param payload Picture data
     * @return observable that completes when request handled
     */
    public Completable setProfilePicture(Blob.Payload payload) {
        return toCompletable(gateway
                .withAuthentication(authenticationContext())
                .setProfilePicture(SetProfilePictureRequest.newBuilder()
                        .setPayload(payload)
                        .build()));
    }

    /**
     * Makes RPC to get default bank account for this member.
     *
     * @return the bank account
     */
    public Observable<Account> getDefaultAccount() {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getDefaultAccount(GetDefaultAccountRequest.newBuilder()
                        .setMemberId(memberId)
                        .build()))
                .map(GetDefaultAccountResponse::getAccount);
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
                .map(response -> response.getAccount()
                        .getId()
                        .contentEquals(accountId));
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
                .map(GetTransferResponse::getTransfer);
    }

    /**
     * Looks up an existing bulk transfer.
     *
     * @param bulkTransferId bulk transfer ID
     * @return bulk transfer record
     */
    public Observable<BulkTransfer> getBulkTransfer(String bulkTransferId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getBulkTransfer(GetBulkTransferRequest
                        .newBuilder()
                        .setBulkTransferId(bulkTransferId)
                        .build()))
                .map(GetBulkTransferResponse::getBulkTransfer);
    }

    /**
     * Looks up an existing Token standing order submission.
     *
     * @param submissionId submission ID
     * @return standing order submission record
     */
    public Observable<StandingOrderSubmission> getStandingOrderSubmission(String submissionId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getStandingOrderSubmission(Gateway.GetStandingOrderSubmissionRequest
                        .newBuilder()
                        .setSubmissionId(submissionId)
                        .build()))
                .map(GetStandingOrderSubmissionResponse::getSubmission);
    }

    /**
     * Looks up a list of existing transfers.
     *
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @param tokenId optional token id to restrict the search
     * @return transfer records
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
                .map(response -> PagedList.create(
                        response.getTransfersList(),
                        response.getOffset()));
    }

    /**
     * Looks up a list of existing standing order submissions.
     *
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @return standing order submissions
     */
    public Observable<PagedList<StandingOrderSubmission, String>> getStandingOrderSubmissions(
            @Nullable String offset,
            int limit) {
        GetStandingOrderSubmissionsRequest request = GetStandingOrderSubmissionsRequest.newBuilder()
                .setPage(pageBuilder(offset, limit))
                .build();

        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getStandingOrderSubmissions(request))
                .map(response -> PagedList.create(
                        response.getSubmissionsList(),
                        response.getOffset()));
    }

    /**
     * Prepare a token, resolving the payload and determining the policy.
     *
     * @param payload token payload
     * @return resolved payload and policy
     */
    public Observable<PrepareTokenResult> prepareToken(TokenPayload payload) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .prepareToken(PrepareTokenRequest.newBuilder()
                        .setPayload(payload)
                        .build()))
                .map(response -> PrepareTokenResult.create(
                        response.getResolvedPayload(),
                        response.getPolicy()));
    }

    /**
     * Creates a new token.
     *
     * @param payload token payload
     * @param tokenRequestId token request ID
     * @param signatures list of token payload signatures
     * @return token returned by server
     */
    public Observable<Token> createToken(
            TokenPayload payload,
            @Nullable String tokenRequestId,
            List<Signature> signatures) {
        CreateTokenRequest.Builder request = CreateTokenRequest.newBuilder()
                .setPayload(payload);
        if (tokenRequestId != null) {
            request.setTokenRequestId(tokenRequestId);
        }
        if (!signatures.isEmpty()) {
            request.addAllSignatures(signatures);
        }
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .createToken(request.build()))
                .map(CreateTokenResponse::getToken);
    }

    /**
     * Creates a new access token.
     *
     * @param tokenPayload token payload
     * @param tokenRequestId token request id
     * @return token returned by server
     */
    public Observable<Token> createAccessToken(
            TokenPayload tokenPayload,
            @Nullable String tokenRequestId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .createAccessToken(CreateAccessTokenRequest.newBuilder()
                        .setPayload(tokenPayload)
                        .setTokenRequestId(tokenRequestId == null ? "" : tokenRequestId)
                        .build()))
                .map(CreateAccessTokenResponse::getToken);
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
                .map(EndorseTokenResponse::getResult);
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
                .map(CancelTokenResponse::getResult);
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
                .map(GetReceiptContactResponse::getContact);
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
                .map(GetActiveAccessTokenResponse::getToken);
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
                .map(response -> PagedList.create(response.getTokensList(), response.getOffset()));
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
                .map(GetTokenResponse::getToken);
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
                .map(CreateTransferResponse::getTransfer);
    }

    /**
     * Redeems a bulk transfer token.
     *
     * @param tokenId ID of token to redeem
     * @return bulk transfer record
     */
    public Observable<BulkTransfer> createBulkTransfer(String tokenId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .createBulkTransfer(CreateBulkTransferRequest.newBuilder()
                        .setTokenId(tokenId)
                        .build()))
                .map(CreateBulkTransferResponse::getTransfer);
    }

    /**
     * Redeems a standing order token.
     *
     * @param tokenId ID of token to redeem
     * @return standing order submission
     */
    public Observable<StandingOrderSubmission> createStandingOrder(String tokenId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .createStandingOrder(CreateStandingOrderRequest.newBuilder()
                        .setTokenId(tokenId)
                        .build()))
                .map(CreateStandingOrderResponse::getSubmission);
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
                .map(LinkAccountsResponse::getAccountsList);
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
                .map(response -> PagedList.create(
                        response.getNotificationsList(),
                        response.getOffset()));
    }

    /**
     * Updates the status of a notification.
     *
     * @param notificationId the notification id to update
     * @param status the status to update
     * @return nothing
     */
    public Completable updateNotificationStatus(String notificationId, Status status) {
        return toCompletable(gateway
                .withAuthentication(authenticationContext())
                .updateNotificationStatus(UpdateNotificationStatusRequest
                        .newBuilder()
                        .setNotificationId(notificationId)
                        .setStatus(status)
                        .build()));
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
                .map(GetNotificationResponse::getNotification);
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
                .map(SubscribeToNotificationsResponse::getSubscriber);
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
                .map(GetSubscriberResponse::getSubscriber);
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
                .map(GetSubscribersResponse::getSubscribersList);
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
                .map(SignTokenRequestStateResponse::getSignature);
    }

    /**
     * Stores a linking request.
     *
     * @param callbackUrl callback URL
     * @param tokenRequestId token request ID
     * @return linking request ID
     */
    public Observable<String> storeLinkingRequest(
            String callbackUrl,
            String tokenRequestId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .storeLinkingRequest(StoreLinkingRequestRequest.newBuilder()
                        .setCallbackUrl(callbackUrl)
                        .setTokenRequestId(tokenRequestId)
                        .build()))
                .map(StoreLinkingRequestResponse::getLinkingRequestId);
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
     * Sets the app's callback url.
     *
     * @param appCallbackUrl the app callback url to set
     * @return completable
     */
    public Completable setAppCallbackUrl(String appCallbackUrl) {
        return toCompletable(gateway
                .withAuthentication(authenticationContext())
                .setAppCallbackUrl(Gateway.SetAppCallbackUrlRequest.newBuilder()
                        .setAppCallbackUrl(appCallbackUrl)
                        .build()));
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
                .map(ReplaceTokenResponse::getResult);
    }
}
