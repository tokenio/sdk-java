package io.token.rpc;

import static io.token.rpc.util.Converters.toObservable;
import static io.token.util.Util.generateNonce;

import io.token.proto.common.member.MemberProtos.Member;
import io.token.proto.common.member.MemberProtos.MemberAddKeyOperation;
import io.token.proto.common.member.MemberProtos.MemberUpdate;
import io.token.proto.common.notification.NotificationProtos.AddKey;
import io.token.proto.common.notification.NotificationProtos.LinkAccounts;
import io.token.proto.common.notification.NotificationProtos.LinkAccountsAndAddKey;
import io.token.proto.common.notification.NotificationProtos.NotifyBody;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.security.SecurityProtos.Key.Level;
import io.token.proto.common.security.SecurityProtos.SealedMessage;
import io.token.proto.common.security.SecurityProtos.Signature;
import io.token.proto.gateway.Gateway.CreateMemberRequest;
import io.token.proto.gateway.Gateway.CreateMemberResponse;
import io.token.proto.gateway.Gateway.NotifyRequest;
import io.token.proto.gateway.Gateway.NotifyResponse;
import io.token.proto.gateway.Gateway.UpdateMemberRequest;
import io.token.proto.gateway.Gateway.UpdateMemberResponse;
import io.token.proto.gateway.Gateway.UsernameExistsRequest;
import io.token.proto.gateway.Gateway.UsernameExistsResponse;
import io.token.proto.gateway.GatewayServiceGrpc.GatewayServiceFutureStub;
import io.token.security.Keys;
import io.token.security.Signer;
import io.token.util.codec.ByteEncoding;

import java.security.PublicKey;
import java.util.List;
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
     * @param publicKey adds first key to be linked with the member id
     * @param signer the signer
     * @return member information
     */
    public Observable<Member> addFirstKey(String memberId, PublicKey publicKey, Signer signer) {
        MemberUpdate update = MemberUpdate.newBuilder()
                .setMemberId(memberId)
                .setAddKey(MemberAddKeyOperation.newBuilder()
                        .setLevel(Level.PRIVILEGED)
                        .setPublicKey(Keys.serializeKey(publicKey)))
                .build();
        return
                toObservable(gateway.updateMember(UpdateMemberRequest.newBuilder()
                        .setUpdate(update)
                        .setUpdateSignature(Signature.newBuilder()
                                .setMemberId(memberId)
                                .setKeyId(signer.getKeyId())
                                .setSignature(signer.sign(update)))
                        .build()))
                        .map(UpdateMemberResponse::getMember);
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
                        .setBody(NotifyBody.newBuilder()
                                .setAddKey(AddKey.newBuilder()
                                        .setPublicKey(ByteEncoding.serialize(publicKey))
                                        .setName(name)
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
     * @param publicKey public key to be added
     * @return status status of the notification
     */
    public Observable<NotifyStatus> notifyLinkAccountsAndAddKey(
            String username,
            String bankId,
            String bankName,
            List<SealedMessage> accountLinkPayloads,
            byte[] publicKey,
            String name) {
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
                                                .setPublicKey(ByteEncoding.serialize(publicKey))
                                                .setName(name)
                                                .build())
                                        .build())
                                .build())
                        .build()))
                .map(NotifyResponse::getStatus);
    }
}
