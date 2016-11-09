package io.token.rpc;

import io.token.proto.common.member.MemberProtos.Member;
import io.token.proto.common.member.MemberProtos.MemberAddKeyOperation;
import io.token.proto.common.member.MemberProtos.MemberUpdate;
import io.token.proto.common.notification.NotificationProtos;
import io.token.proto.common.notification.NotificationProtos.AddKey;
import io.token.proto.common.notification.NotificationProtos.LinkAccounts;
import io.token.proto.common.notification.NotificationProtos.LinkAccountsAndAddKey;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.security.SecurityProtos.Key.Level;
import io.token.proto.common.security.SecurityProtos.Signature;
import io.token.proto.gateway.Gateway.*;
import io.token.proto.gateway.GatewayServiceGrpc.GatewayServiceFutureStub;
import io.token.security.SecretKey;
import io.token.util.codec.ByteEncoding;
import rx.Observable;

import java.util.List;

import static io.token.rpc.util.Converters.toObservable;
import static io.token.security.Crypto.sign;
import static io.token.util.Util.generateNonce;

/**
 * Similar to {@link Client} but is only used for a handful of requests that
 * don't require authentication. We use this client to create new member or
 * login an existing one and switch to the authenticated {@link Client}.
 */
public final class UnauthenticatedClient {
    private final GatewayServiceFutureStub gateway;

    /**
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
        return toObservable(gateway.usernameExists(UsernameExistsRequest.newBuilder()
                .setUsername(username)
                .build()))
                .map(UsernameExistsResponse::getExists);
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
     * Adds first key to be linked with the specified member id.
     *
     * @param memberId member id
     * @param key adds first key to be linked with the member id
     * @return member information
     */
    public Observable<Member> addFirstKey(String memberId, SecretKey key) {
        MemberUpdate update = MemberUpdate.newBuilder()
                .setMemberId(memberId)
                .setAddKey(MemberAddKeyOperation.newBuilder()
                        .setLevel(Level.PRIVILEGED)
                        .setPublicKey(ByteEncoding.serialize(key.getPublicKey())))
                .build();

        return
                toObservable(gateway.updateMember(UpdateMemberRequest.newBuilder()
                        .setUpdate(update)
                        .setUpdateSignature(Signature.newBuilder()
                                .setMemberId(memberId)
                                .setKeyId(key.getId())
                                .setSignature(sign(key, update)))
                        .build()))
                        .map(UpdateMemberResponse::getMember);
    }

    /**
     * Notifies subscribed devices that accounts should be linked
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
            List<String> accountLinkPayloads) {
        return toObservable(gateway.notify(
                NotifyRequest.newBuilder()
                        .setUsername(username)
                        .setNotification(NotificationProtos.Notification.newBuilder()
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
     * Notifies subscribed devices that a key should be added
     *
     * @param username username of the member
     * @param publicKey public key to be added
     * @return status status of the notification
     */
    public Observable<NotifyStatus> notifyAddKey(
            String username,
            byte[] publicKey,
            String name) {
        return toObservable(gateway.notify(
                NotifyRequest.newBuilder()
                        .setUsername(username)
                        .setNotification(NotificationProtos.Notification.newBuilder()
                                .setAddKey(AddKey.newBuilder()
                                        .setPublicKey(ByteEncoding.serialize(publicKey))
                                        .setName(name)
                                        .build())
                                .build())
                        .build()))
                .map(NotifyResponse::getStatus);
    }

    /**
     * Notifies subscribed devices that a key should be added
     *
     * @param username username of the member
     * @param bankId id of the bank owning the accounts
     * @param bankName name of the bank owning the accounts
     * @param accountLinkPayloads a list of account payloads to be linked
     * @param publicKey public key to be added
     * @return status status of the notification
     */
    public Observable<NotifyStatus> notifyLinkAccountsAndAddKey(
            String username,
            String bankId,
            String bankName,
            List<String> accountLinkPayloads,
            byte[] publicKey,
            String name) {
        return toObservable(gateway.notify(
                NotifyRequest.newBuilder()
                        .setUsername(username)
                        .setNotification(NotificationProtos.Notification.newBuilder()
                                .setLinkAccountsAndAddKey(LinkAccountsAndAddKey.newBuilder()
                                        .setLinkAccounts(LinkAccounts.newBuilder()
                                                .setBankId(bankId)
                                                .setBankName(bankName)
                                                .addAllAccountLinkPayloads(accountLinkPayloads)
                                                .build())
                                        .setAddKey(AddKey.newBuilder()
                                                .setPublicKey(ByteEncoding.serialize(publicKey))
                                                .setName(name)
                                                .build())
                                        .build())
                                .build())
                        .build()))
                .map(NotifyResponse::getStatus);
    }
}
