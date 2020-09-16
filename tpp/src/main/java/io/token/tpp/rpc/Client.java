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

package io.token.tpp.rpc;

import static io.grpc.Status.INVALID_ARGUMENT;
import static io.token.proto.common.token.TokenProtos.TokenSignature.Action.CANCELLED;
import static io.token.rpc.util.Converters.toCompletable;
import static io.token.util.Util.toObservable;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.token.proto.PagedList;
import io.token.proto.common.blob.BlobProtos.Blob;
import io.token.proto.common.eidas.EidasProtos.VerifyEidasPayload;
import io.token.proto.common.member.MemberProtos.Profile;
import io.token.proto.common.notification.NotificationProtos;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.security.SecurityProtos.CustomerTrackingMetadata;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.security.SecurityProtos.Signature;
import io.token.proto.common.submission.SubmissionProtos.StandingOrderSubmission;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.proto.common.token.TokenProtos.TokenRequestOptions;
import io.token.proto.common.token.TokenProtos.TokenRequestPayload;
import io.token.proto.common.transfer.TransferProtos;
import io.token.proto.common.transfer.TransferProtos.BulkTransfer;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferDestination;
import io.token.proto.common.webhook.WebhookProtos.Webhook.Config;
import io.token.proto.gateway.Gateway;
import io.token.proto.gateway.Gateway.AddRedirectUrlsRequest;
import io.token.proto.gateway.Gateway.CancelTokenRequest;
import io.token.proto.gateway.Gateway.CancelTokenResponse;
import io.token.proto.gateway.Gateway.CreateCustomizationRequest;
import io.token.proto.gateway.Gateway.CreateCustomizationResponse;
import io.token.proto.gateway.Gateway.CreateStandingOrderResponse;
import io.token.proto.gateway.Gateway.CreateTransferRequest;
import io.token.proto.gateway.Gateway.CreateTransferResponse;
import io.token.proto.gateway.Gateway.DeleteWebhookConfigRequest;
import io.token.proto.gateway.Gateway.GetActiveAccessTokenRequest;
import io.token.proto.gateway.Gateway.GetActiveAccessTokenResponse;
import io.token.proto.gateway.Gateway.GetBankAuthUrlRequest;
import io.token.proto.gateway.Gateway.GetBankAuthUrlResponse;
import io.token.proto.gateway.Gateway.GetBlobRequest;
import io.token.proto.gateway.Gateway.GetEidasCertificateStatusRequest;
import io.token.proto.gateway.Gateway.GetEidasCertificateStatusResponse;
import io.token.proto.gateway.Gateway.GetEidasVerificationStatusRequest;
import io.token.proto.gateway.Gateway.GetEidasVerificationStatusResponse;
import io.token.proto.gateway.Gateway.GetExternalMetadataRequest;
import io.token.proto.gateway.Gateway.GetExternalMetadataResponse;
import io.token.proto.gateway.Gateway.GetRedirectUrlsRequest;
import io.token.proto.gateway.Gateway.GetRedirectUrlsResponse;
import io.token.proto.gateway.Gateway.GetStandingOrderSubmissionsRequest;
import io.token.proto.gateway.Gateway.GetTokenRequest;
import io.token.proto.gateway.Gateway.GetTokenResponse;
import io.token.proto.gateway.Gateway.GetTokensRequest;
import io.token.proto.gateway.Gateway.GetTransferRequest;
import io.token.proto.gateway.Gateway.GetTransferResponse;
import io.token.proto.gateway.Gateway.GetTransfersRequest;
import io.token.proto.gateway.Gateway.GetWebhookConfigRequest;
import io.token.proto.gateway.Gateway.GetWebhookConfigResponse;
import io.token.proto.gateway.Gateway.OnBankAuthCallbackRequest;
import io.token.proto.gateway.Gateway.OnBankAuthCallbackResponse;
import io.token.proto.gateway.Gateway.RemoveRedirectUrlsRequest;
import io.token.proto.gateway.Gateway.SetProfilePictureRequest;
import io.token.proto.gateway.Gateway.SetProfileRequest;
import io.token.proto.gateway.Gateway.SetTokenRequestTransferDestinationsRequest;
import io.token.proto.gateway.Gateway.SetWebhookConfigRequest;
import io.token.proto.gateway.Gateway.StoreTokenRequestRequest;
import io.token.proto.gateway.Gateway.TriggerStepUpNotificationRequest;
import io.token.proto.gateway.Gateway.TriggerStepUpNotificationResponse;
import io.token.proto.gateway.Gateway.VerifyEidasRequest;
import io.token.proto.gateway.Gateway.VerifyEidasResponse;
import io.token.rpc.GatewayProvider;
import io.token.security.CryptoEngine;
import io.token.security.Signer;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * An authenticated RPC client that is used to talk to Token gateway. The
 * class is a thin wrapper on top of gRPC generated client. Makes the API
 * easier to use.
 */
public final class Client extends io.token.rpc.Client {
    private String onBehalfOf;

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
                .map(Gateway.SetProfileResponse::getProfile);
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
                .map(Gateway.GetBlobResponse::getBlob);
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
        return updated;
    }

    /**
     * Creates a new instance with On-Behalf-Of authentication set.
     *
     * @param tokenId access token ID to be used
     * @param customerTrackingMetadata customer tracking metadata
     * @return new client instance
     */
    public Client forAccessToken(
            String tokenId,
            CustomerTrackingMetadata customerTrackingMetadata) {
        if (customerTrackingMetadata.equals(CustomerTrackingMetadata.getDefaultInstance())) {
            throw INVALID_ARGUMENT
                    .withDescription(
                            "User tracking metadata is empty. "
                                    + "Use forAccessToken(String, boolean) instead.")
                    .asRuntimeException();
        }
        Client updated = new Client(memberId, crypto, gateway);
        updated.useAccessToken(tokenId, customerTrackingMetadata);
        return updated;
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
    private void useAccessToken(String accessTokenId, boolean customerInitiated) {
        this.onBehalfOf = accessTokenId;
        this.customerInitiated = customerInitiated;
    }

    /**
     * Sets the On-Behalf-Of authentication value to be used
     * with this client.  The value must correspond to an existing
     * Access Token ID issued for the client member. Uses the given customer
     * initiated flag.
     *
     * @param accessTokenId the access token id to be used
     * @param customerTrackingMetadata the tracking metadata of the customer
     */
    private void useAccessToken(
            String accessTokenId,
            CustomerTrackingMetadata customerTrackingMetadata) {
        this.onBehalfOf = accessTokenId;
        this.customerInitiated = true;
        this.customerTrackingMetadata = customerTrackingMetadata;
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
                .map(response -> response.getTokenRequest().getId());
    }

    /**
     * Sets destination accounts for once if it hasn't been set.
     *
     * @param tokenRequestId token request Id
     * @param transferDestinations destination accounts
     * @return observable that completes when request handled
     */
    public Completable setTokenRequestTransferDestinations(
            String tokenRequestId,
            List<TransferDestination> transferDestinations) {
        return toCompletable(gateway
                .withAuthentication(authenticationContext())
                .setTokenRequestTransferDestinations(
                        SetTokenRequestTransferDestinationsRequest
                                .newBuilder()
                                .setTokenRequestId(tokenRequestId)
                                .addAllTransferDestinations(transferDestinations)
                                .build()));
    }

    /**
     * Creates a customization.
     *
     * @param logo logo
     * @param colors map of ARGB colors #AARRGGBB
     * @param consentText consent text
     * @param name display name
     * @param appName corresponding app name
     * @return customization id
     */
    public Observable<String> createCustomization(
            Blob.Payload logo,
            Map<String, String> colors,
            String consentText,
            String name,
            String appName) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .createCustomization(CreateCustomizationRequest.newBuilder()
                        .setLogo(logo)
                        .putAllColors(colors)
                        .setConsentText(consentText)
                        .setName(name)
                        .setAppName(appName)
                        .build()))
                .map(CreateCustomizationResponse::getCustomizationId);
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
                .getBulkTransfer(Gateway.GetBulkTransferRequest
                        .newBuilder()
                        .setBulkTransferId(bulkTransferId)
                        .build()))
                .map(Gateway.GetBulkTransferResponse::getBulkTransfer);
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
                .map(Gateway.GetStandingOrderSubmissionResponse::getSubmission);
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
     * Redeems a transfer token.
     *
     * @param transfer transfer parameters, such as amount, currency, etc
     * @return transfer record
     */
    public Observable<Transfer> createTransfer(TransferProtos.TransferPayload transfer) {
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
                .createBulkTransfer(Gateway.CreateBulkTransferRequest.newBuilder()
                        .setTokenId(tokenId)
                        .build()))
                .map(Gateway.CreateBulkTransferResponse::getTransfer);
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
                .createStandingOrder(Gateway.CreateStandingOrderRequest.newBuilder()
                        .setTokenId(tokenId)
                        .build()))
                .map(CreateStandingOrderResponse::getSubmission);
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
                .map(TriggerStepUpNotificationResponse::getStatus);
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
                .map(TriggerStepUpNotificationResponse::getStatus);
    }

    /**
     * Verifies eIDAS certificate.
     *
     * @param payload payload containing member id, eIDAS alias and the certificate
     * @param signature payload signed with the private key corresponding to the certificate
     * @return result of the verification operation, returned by the server
     */
    public Observable<VerifyEidasResponse> verifyEidas(
            VerifyEidasPayload payload,
            String signature) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .verifyEidas(VerifyEidasRequest.newBuilder()
                        .setPayload(payload)
                        .setSignature(signature)
                        .build()));
    }

    /**
     * Get status of the current eIDAS certificate along with the certificate itself.
     *
     * @return eidas status and the eidas certificate, if any
     */
    public Observable<GetEidasCertificateStatusResponse> getEidasCertificateStatus() {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getEidasCertificateStatus(GetEidasCertificateStatusRequest.getDefaultInstance()));
    }

    /**
     * Retrieves an eIDAS verification status by verificationId.
     *
     * @param verificationId verification id
     * @return a status of the verification operation together with the certificate and alias value
     */
    public Observable<GetEidasVerificationStatusResponse> getEidasVerificationStatus(
            String verificationId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getEidasVerificationStatus(GetEidasVerificationStatusRequest.newBuilder()
                        .setVerificationId(verificationId)
                        .build()));
    }

    /**
     * Get url to bank authorization page for a token request.
     *
     * @param bankId bank ID
     * @param tokenRequestId token request ID
     * @param customerTrackingMetadata customer tracking metadata
     * @return url
     */
    public Observable<String> getBankAuthUrl(
            String bankId,
            String tokenRequestId,
            Optional<CustomerTrackingMetadata> customerTrackingMetadata) {
        return toObservable(gateway
                .withAuthentication(customerTrackingMetadata
                        .map(this::authenticationContext)
                        .orElseGet(this::authenticationContext))
                .getBankAuthUrl(GetBankAuthUrlRequest.newBuilder()
                        .setBankId(bankId)
                        .setTokenRequestId(tokenRequestId)
                        .build()))
                .map(GetBankAuthUrlResponse::getUrl);
    }

    /**
     * Forward the callback from the bank (after user authentication) to Token.
     *
     * @param bankId bank ID
     * @param query HTTP query string
     * @return token request ID
     */
    public Observable<String> onBankAuthCallback(String bankId, String query) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .onBankAuthCallback(OnBankAuthCallbackRequest.newBuilder()
                        .setBankId(bankId)
                        .setQuery(query)
                        .build()))
                .map(OnBankAuthCallbackResponse::getTokenRequestId);
    }

    /**
     * Get the external metadata from the bank associated with a token request.
     *
     * @param tokenRequestId token request ID
     * @return external metadata
     */
    public Observable<GetExternalMetadataResponse> getExternalMetadata(String tokenRequestId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getExternalMetadata(GetExternalMetadataRequest.newBuilder()
                        .setTokenRequestId(tokenRequestId)
                        .build()));
    }

    /**
     * Set a webhook config.
     *
     * @param config the webhook config
     * @return completable
     */
    public Completable setWebhookConfig(Config config) {
        return toCompletable(gateway
                .withAuthentication(authenticationContext())
                .setWebhookConfig(SetWebhookConfigRequest.newBuilder()
                        .setConfig(config)
                        .build()));
    }

    /**
     * Get the webhook config.
     *
     * @return config
     */
    public Observable<Config> getWebhookConfig() {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getWebhookConfig(GetWebhookConfigRequest.getDefaultInstance()))
                .map(GetWebhookConfigResponse::getConfig);
    }

    /**
     * delete the webhook config.
     *
     * @return completable
     */
    public Completable deleteWebhookConfig() {
        return toCompletable(gateway
                .withAuthentication(authenticationContext())
                .deleteWebhookConfig(DeleteWebhookConfigRequest.getDefaultInstance()));
    }

    /**
     * Get redirect URLs.
     *
     * @return redirect URLs
     */
    public Observable<List<String>> getRedirectUrls() {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getRedirectUrls(GetRedirectUrlsRequest.getDefaultInstance()))
                .map(GetRedirectUrlsResponse::getRedirectUrlsList);
    }

    /**
     * Add redirect URLs.
     *
     * @param redirectUrls redirect URLs to add
     * @return completable
     */
    public Completable addRedirectUrls(List<String> redirectUrls) {
        return toCompletable(gateway
                .withAuthentication(authenticationContext())
                .addRedirectUrls(AddRedirectUrlsRequest.newBuilder()
                        .addAllRedirectUrls(redirectUrls)
                        .build()));
    }

    /**
     * Remove redirect URLs.
     *
     * @param redirectUrls redirect URLs to remove
     * @return completable
     */
    public Completable removeRedirectUrls(List<String> redirectUrls) {
        return toCompletable(gateway
                .withAuthentication(authenticationContext())
                .removeRedirectUrls(RemoveRedirectUrlsRequest.newBuilder()
                        .addAllRedirectUrls(redirectUrls)
                        .build()));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof io.token.rpc.Client)) {
            return false;
        }

        Client other = (Client) obj;
        return memberId.equals(other.memberId)
                && Objects.equals(onBehalfOf, other.onBehalfOf);
    }

    @Override
    public int hashCode() {
        return (memberId + (onBehalfOf == null ? "" : onBehalfOf)).hashCode();
    }

    @Override
    protected String getOnBehalfOf() {
        return onBehalfOf;
    }
}
