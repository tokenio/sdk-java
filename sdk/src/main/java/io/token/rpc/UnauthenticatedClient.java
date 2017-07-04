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

import static io.token.util.Util.generateNonce;
import static io.token.util.Util.toObservable;

import com.google.common.base.Strings;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.proto.common.member.MemberProtos.Member;
import io.token.proto.common.member.MemberProtos.MemberOperation;
import io.token.proto.common.member.MemberProtos.MemberUpdate;
import io.token.proto.common.notification.NotificationProtos;
import io.token.proto.common.notification.NotificationProtos.AddKey;
import io.token.proto.common.notification.NotificationProtos.LinkAccounts;
import io.token.proto.common.notification.NotificationProtos.LinkAccountsAndAddKey;
import io.token.proto.common.notification.NotificationProtos.NotifyBody;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.notification.NotificationProtos.PaymentRequest;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.security.SecurityProtos.Signature;
import io.token.proto.common.token.TokenProtos;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.proto.gateway.Gateway.CreateMemberRequest;
import io.token.proto.gateway.Gateway.CreateMemberResponse;
import io.token.proto.gateway.Gateway.GetMemberIdRequest;
import io.token.proto.gateway.Gateway.GetMemberIdResponse;
import io.token.proto.gateway.Gateway.NotifyRequest;
import io.token.proto.gateway.Gateway.NotifyResponse;
import io.token.proto.gateway.Gateway.UpdateMemberRequest;
import io.token.proto.gateway.Gateway.UpdateMemberResponse;
import io.token.proto.gateway.GatewayServiceGrpc.GatewayServiceFutureStub;
import io.token.security.Signer;

import java.util.List;

import rx.Observable;
import rx.functions.Func1;

/**
 * Similar to {@link Client} but is only used for a handful of requests that
 * don't require authentication. We use this client to create new member or
 * login an existing one and switch to the authenticated {@link Client}.
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
     * Checks if a given username already exists.
     *
     * @param username username to check
     * @return {@code true} if username already exists, {@code false} otherwise
     */
    public Observable<Boolean> usernameExists(String username) {
        return toObservable(gateway
                .getMemberId(GetMemberIdRequest
                        .newBuilder()
                        .setUsername(username)
                        .build()))
                .map(new Func1<GetMemberIdResponse, Boolean>() {
                    public Boolean call(GetMemberIdResponse response) {
                        return !response.getMemberId().isEmpty();
                    }
                });
    }

    /**
     * Looks up member id for a given username.
     *
     * @param username username to check
     * @return member id if username already exists, null otherwise
     */
    public Observable<String> getMemberId(String username) {
        return toObservable(
                gateway.getMemberId(GetMemberIdRequest.newBuilder()
                        .setUsername(username)
                        .build()))
                .map(new Func1<GetMemberIdResponse, String>() {
                    public String call(GetMemberIdResponse response) {
                        return Strings.emptyToNull(response.getMemberId());
                    }
                });
    }

    /**
     * Creates new member ID. After the method returns the ID is reserved on the server.
     *
     * @return newly created member id
     */
    public Observable<String> createMemberId() {
        return
                toObservable(gateway.createMember(CreateMemberRequest.newBuilder()
                        .setNonce(generateNonce())
                        .build()))
                        .map(new Func1<CreateMemberResponse, String>() {
                            public String call(CreateMemberResponse response) {
                                return response.getMemberId();
                            }
                        });
    }

    /**
     * Creates a new token member.
     *
     * @param memberId member ID
     * @param operations operations to set up member keys and usernames
     * @param signer the signer used to sign the requests
     * @return member information
     */
    public Observable<Member> createMember(
            String memberId,
            List<MemberOperation> operations,
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
                .build()))
                .map(new Func1<UpdateMemberResponse, Member>() {
                    public Member call(UpdateMemberResponse response) {
                        return response.getMember();
                    }
                });
    }

    /**
     * Notifies subscribed devices that accounts should be linked.
     *
     * @param username username of the member
     * @param authorization the bank authorization for the funding account
     * @return status status of the notification
     */
    public Observable<NotifyStatus> notifyLinkAccounts(
            String username,
            BankAuthorization authorization) {
        return toObservable(gateway.notify(
                NotifyRequest.newBuilder()
                        .setUsername(username)
                        .setBody(NotifyBody.newBuilder()
                                .setLinkAccounts(LinkAccounts.newBuilder()
                                        .setBankAuthorization(authorization)
                                        .build())
                                .build())
                        .build()))
                .map(new Func1<NotifyResponse, NotifyStatus>() {
                    public NotifyStatus call(NotifyResponse response) {
                        return response.getStatus();
                    }
                });
    }


    /**
     * Notifies subscribed devices that a key should be added.
     *
     * @param username username of the member
     * @param name device/client name, e.g. iPhone, Chrome Browser, etc
     * @param key the that needs an approval
     * @return status status of the notification
     */
    public Observable<NotifyStatus> notifyAddKey(
            String username,
            String name,
            Key key) {
        return toObservable(gateway.notify(
                NotifyRequest.newBuilder()
                        .setUsername(username)
                        .setBody(NotifyBody.newBuilder()
                                .setAddKey(AddKey.newBuilder()
                                        .setName(name)
                                        .setKey(key)
                                        .build())
                                .build())
                        .build()))
                .map(new Func1<NotifyResponse, NotifyStatus>() {
                    public NotifyStatus call(NotifyResponse response) {
                        return response.getStatus();
                    }
                });
    }

    /**
     * Notifies subscribed devices that a key should be added.
     *
     * @param username username of the member
     * @param authorization the bank authorization for the funding account
     * @param name device/client name, e.g. iPhone, Chrome Browser, etc
     * @param key the that needs an approval
     * @return status status of the notification
     */
    public Observable<NotifyStatus> notifyLinkAccountsAndAddKey(
            String username,
            BankAuthorization authorization,
            String name,
            Key key) {
        return toObservable(gateway.notify(
                NotifyRequest.newBuilder()
                        .setUsername(username)
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
                .map(new Func1<NotifyResponse, NotifyStatus>() {
                    public NotifyStatus call(NotifyResponse response) {
                        return response.getStatus();
                    }
                });
    }

    /**
     * Notifies subscribed devices that on payment requests.
     *
     * @param username the username of the member to notify
     * @param tokenPayload the pyload of a token to be sent
     * @return status of the notification request
     */
    public Observable<NotifyStatus> notifyPaymentRequest(
            String username,
            TokenPayload tokenPayload) {
        return toObservable(gateway.notify(
                NotifyRequest.newBuilder()
                        .setUsername(username)
                        .setBody(NotifyBody.newBuilder()
                                .setPaymentRequest(PaymentRequest.newBuilder()
                                        .setPayload(tokenPayload)
                                        .build())
                                .build())
                        .build()))
                .map(new Func1<NotifyResponse, NotifyStatus>() {
                    public NotifyStatus call(NotifyResponse response) {
                        return response.getStatus();
                    }
                });
    }
}
