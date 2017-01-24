package io.token.rpc;

import static io.token.rpc.util.Converters.toObservable;
import static io.token.util.Util.generateNonce;

import com.google.common.base.Strings;
import io.token.proto.common.member.MemberProtos.Member;
import io.token.proto.common.member.MemberProtos.MemberAddKeyOperation;
import io.token.proto.common.member.MemberProtos.MemberOperation;
import io.token.proto.common.member.MemberProtos.MemberUpdate;
import io.token.proto.common.notification.NotificationProtos.AddKey;
import io.token.proto.common.notification.NotificationProtos.LinkAccounts;
import io.token.proto.common.notification.NotificationProtos.LinkAccountsAndAddKey;
import io.token.proto.common.notification.NotificationProtos.NotifyBody;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.security.SecurityProtos.SealedMessage;
import io.token.proto.common.security.SecurityProtos.Signature;
import io.token.proto.gateway.Gateway;
import io.token.proto.gateway.Gateway.CreateMemberRequest;
import io.token.proto.gateway.Gateway.CreateMemberResponse;
import io.token.proto.gateway.Gateway.GetMemberIdRequest;
import io.token.proto.gateway.Gateway.NotifyRequest;
import io.token.proto.gateway.Gateway.NotifyResponse;
import io.token.proto.gateway.Gateway.UpdateMemberRequest;
import io.token.proto.gateway.GatewayServiceGrpc.GatewayServiceFutureStub;
import io.token.security.Signer;

import java.util.List;
import java.util.stream.Collectors;
import rx.Observable;

/**
 * Similar to {@link Client} but is only used for a handful of requests that
 * don't require authentication. We use this client to create new member or
 * login an existing one and switch to the authenticated {@link Client}.
 */
public final class UnauthenticatedClient {
    private final GatewayServiceFutureStub gateway;

    /**
     * @param gateway gateway gRPC stub.
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
        return toObservable(
                gateway.getMemberId(GetMemberIdRequest.newBuilder()
                        .setUsername(username)
                        .build()))
                .map(res -> !res.getMemberId().isEmpty());
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
                .map(res -> Strings.emptyToNull(res.getMemberId()));
    }

    /**
     * Creates new member ID. After the method returns the ID is reserved on
     * the server.
     *
     * @return newly created member id
     */
    public Observable<String> createMemberId() {
        return
                toObservable(gateway.createMember(CreateMemberRequest.newBuilder()
                        .setNonce(generateNonce())
                        .build()))
                        .map(CreateMemberResponse::getMemberId);
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
                .map(Gateway.UpdateMemberResponse::getMember);
    }

    /**
     * Notifies subscribed devices that accounts should be linked.
     *
     * @param username username of the member
     * @param bankId id of the bank owning the accounts
     * @param bankName name of the bank owning the accounts
     * @param accountLinkPayloads a list of account payloads to be linked
     * @return status status of the notification
     */
    public Observable<NotifyStatus> notifyLinkAccounts(
            String username,
            String bankId,
            String bankName,
            List<SealedMessage> accountLinkPayloads) {
        return toObservable(gateway.notify(
                NotifyRequest.newBuilder()
                        .setUsername(username)
                        .setBody(NotifyBody.newBuilder()
                                .setLinkAccounts(LinkAccounts.newBuilder()
                                        .setBankId(bankId)
                                        .setBankName(bankName)
                                        .addAllAccountLinkPayloads(accountLinkPayloads)
                                        .build())
                                .build())
                        .build()))
                .map(NotifyResponse::getStatus);
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
                .map(NotifyResponse::getStatus);
    }

    /**
     * Notifies subscribed devices that a key should be added.
     *
     * @param username username of the member
     * @param bankId id of the bank owning the accounts
     * @param bankName name of the bank owning the accounts
     * @param accountLinkPayloads a list of account payloads to be linked
     * @param name device/client name, e.g. iPhone, Chrome Browser, etc
     * @param key the that needs an approval
     * @return status status of the notification
     */
    public Observable<NotifyStatus> notifyLinkAccountsAndAddKey(
            String username,
            String bankId,
            String bankName,
            List<SealedMessage> accountLinkPayloads,
            String name,
            Key key) {
        return toObservable(gateway.notify(
                NotifyRequest.newBuilder()
                        .setUsername(username)
                        .setBody(NotifyBody.newBuilder()
                                .setLinkAccountsAndAddKey(LinkAccountsAndAddKey.newBuilder()
                                        .setLinkAccounts(LinkAccounts.newBuilder()
                                                .setBankId(bankId)
                                                .setBankName(bankName)
                                                .addAllAccountLinkPayloads(accountLinkPayloads)
                                                .build())
                                        .setAddKey(AddKey.newBuilder()
                                                .setName(name)
                                                .setKey(key)
                                                .build())
                                        .build())
                                .build())
                        .build()))
                .map(NotifyResponse::getStatus);
    }
}
