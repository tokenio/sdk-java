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

import static io.token.rpc.util.Converters.toCompletable;
import static io.token.util.Util.toObservable;

import com.google.common.base.Strings;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.blob.BlobProtos;
import io.token.proto.common.member.MemberProtos.ReceiptContact;
import io.token.proto.common.notification.NotificationProtos.AddKey;
import io.token.proto.common.notification.NotificationProtos.NotifyBody;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.proto.common.token.TokenProtos.TokenRequestOptions;
import io.token.proto.gateway.Gateway;
import io.token.proto.gateway.Gateway.GetBlobResponse;
import io.token.proto.gateway.Gateway.GetTokenRequestResultResponse;
import io.token.proto.gateway.Gateway.InvalidateNotificationRequest;
import io.token.proto.gateway.Gateway.InvalidateNotificationResponse;
import io.token.proto.gateway.Gateway.RequestTransferRequest;
import io.token.proto.gateway.Gateway.RequestTransferResponse;
import io.token.proto.gateway.Gateway.RetrieveTokenRequestResponse;
import io.token.proto.gateway.Gateway.TriggerCreateAndEndorseTokenNotificationRequest;
import io.token.proto.gateway.Gateway.TriggerCreateAndEndorseTokenNotificationResponse;
import io.token.proto.gateway.Gateway.UpdateTokenRequestRequest;
import io.token.proto.gateway.GatewayServiceGrpc.GatewayServiceFutureStub;
import io.token.tokenrequest.TokenRequest;
import io.token.tokenrequest.TokenRequestResult;
import io.token.user.NotifyResult;

import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Similar to {@link io.token.user.rpc.Client} but is only used for a handful of requests that
 * don't require authentication. We use this client to create new member or
 * getMember an existing one and switch to the authenticated {@link Client}.
 */
public final class UnauthenticatedClient extends io.token.rpc.UnauthenticatedClient {
    /**
     * Creates an instance.
     *
     * @param gateway gateway gRPC stub
     */
    public UnauthenticatedClient(GatewayServiceFutureStub gateway) {
        super(gateway);
    }

    /**
     * Retrieves a blob from the server.
     *
     * @param blobId id of the blob
     * @return Blob
     */
    public Observable<BlobProtos.Blob> getBlob(String blobId) {
        return toObservable(gateway
                .getBlob(Gateway.GetBlobRequest
                        .newBuilder()
                        .setBlobId(blobId)
                        .build()))
                .map(GetBlobResponse::getBlob);
    }

    /**
     * Notifies subscribed devices that a key should be added.
     *
     * @param alias alias of the member
     * @param addKey the add key payload to be sent
     * @return status status of the notification
     */
    public Observable<NotifyStatus> notifyAddKey(Alias alias, AddKey addKey) {
        return toObservable(gateway.notify(
                Gateway.NotifyRequest.newBuilder()
                        .setAlias(alias)
                        .setBody(NotifyBody.newBuilder()
                                .setAddKey(addKey)
                                .build())
                        .build()))
                .map(Gateway.NotifyResponse::getStatus);
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
                .map(RequestTransferResponse::getStatus);
    }

    /**
     * Notifies subscribed devices that a token should be created and endorsed.
     *
     * @param tokenRequestId the token request ID to send
     * @param addKey optional add key payload to send
     * @param receiptContact optional receipt contact to send
     * @return notify result of the notification request
     */
    public Observable<NotifyResult> notifyCreateAndEndorseToken(
            String tokenRequestId,
            @Nullable AddKey addKey,
            @Nullable ReceiptContact receiptContact) {
        TriggerCreateAndEndorseTokenNotificationRequest.Builder builder =
                TriggerCreateAndEndorseTokenNotificationRequest.newBuilder()
                        .setTokenRequestId(tokenRequestId);

        if (addKey != null) {
            builder.setAddKey(addKey);
        }

        if (receiptContact != null) {
            builder.setContact(receiptContact);
        }

        return toObservable(gateway.triggerCreateAndEndorseTokenNotification(builder.build()))
                .map(response -> NotifyResult.create(
                        response.getNotificationId(),
                        response.getStatus()));
    }

    /**
     * Invalidate a notification.
     *
     * @param notificationId notification id to invalidate
     * @return status of the invalidation request
     */
    public Observable<NotifyStatus> invalidateNotification(String notificationId) {
        return toObservable(gateway.invalidateNotification(
                InvalidateNotificationRequest.newBuilder()
                        .setNotificationId(notificationId)
                        .build()))
                .map(InvalidateNotificationResponse::getStatus);
    }

    /**
     * Get the token request result based on a token's tokenRequestId.
     *
     * @param tokenRequestId token request id
     * @return token request result
     */
    public Observable<TokenRequestResult> getTokenRequestResult(String tokenRequestId) {
        return toObservable(gateway
                .getTokenRequestResult(Gateway.GetTokenRequestResultRequest.newBuilder()
                        .setTokenRequestId(tokenRequestId)
                        .build()))
                .map(response -> TokenRequestResult.create(
                        response.getTokenId(),
                        Optional.ofNullable(Strings.emptyToNull(response.getTransferId())),
                        response.getSignature()));
    }

    /**
     * Retrieves a transfer token request.
     *
     * @param tokenRequestId token request id
     *
     * @return token request that was stored with the request id
     */
    public Observable<TokenRequest> retrieveTokenRequest(String tokenRequestId) {
        return toObservable(gateway.retrieveTokenRequest(Gateway.RetrieveTokenRequestRequest
                .newBuilder()
                .setRequestId(tokenRequestId)
                .build()))
                .map(response -> TokenRequest
                        .fromProtos(
                                response.getTokenRequest().getRequestPayload(),
                                response.getTokenRequest().getRequestOptions()));
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
                .updateTokenRequest(UpdateTokenRequestRequest.newBuilder()
                        .setRequestId(requestId)
                        .setRequestOptions(options)
                        .build()));
    }
}
