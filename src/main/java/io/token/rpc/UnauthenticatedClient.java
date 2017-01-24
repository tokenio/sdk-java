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
import io.token.proto.gateway.Gateway.CreateMemberRequest;
import io.token.proto.gateway.Gateway.CreateMemberResponse;
import io.token.proto.gateway.Gateway.GetMemberIdRequest;
import io.token.proto.gateway.Gateway.NotifyRequest;
import io.token.proto.gateway.Gateway.NotifyResponse;
import io.token.proto.gateway.Gateway.UpdateMemberRequest;
import io.token.proto.gateway.GatewayServiceGrpc.GatewayServiceFutureStub;
import io.token.rpc.util.Converters;
import io.token.security.Signer;

import java.util.List;
import java.util.Optional;
import rx.Observable;
import rx.Single;
import rx.SingleSubscriber;

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
     * Adds keys to be linked with the specified member id.
     *
     * @param memberId member id
     * @param keys keys to add add for the given member
     * @param signer the signer used to sign the requests
     * @return member information
     */
    public Observable<Member> addKeys(String memberId, List<Key> keys, Signer signer) {
        return Single
                .<Member>create(subscriber -> addAllKeys(
                        memberId,
                        keys,
                        0,
                        Optional.empty(),
                        signer,
                        subscriber))
                .toObservable();
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

    /**
     * Adds one of the keys in the list to the member. If we have not reached
     * the end of the list yet then we invoke this function recursively to
     * add the next key. Otherwise, we stop and invoke the callback.
     *
     * @param memberId member id
     * @param keys keys to be added
     * @param keyIndex key being added with this invocation
     * @param lastHash last hash of the directory entry
     * @param signer signer to use to sign the messages
     * @param callback callback to invoke when all the keys has been added
     */
    // TODO: Replace this with batch call when it is available.
    private void addAllKeys(
            String memberId,
            List<Key> keys,
            int keyIndex,
            Optional<String> lastHash,
            Signer signer,
            SingleSubscriber<? super Member> callback) {
        Key key = keys.get(keyIndex);

        MemberUpdate.Builder update = MemberUpdate.newBuilder()
                .setMemberId(memberId)
                .addOperations(MemberOperation.newBuilder()
                        .setAddKey(MemberAddKeyOperation.newBuilder()
                                .setKey(key)));
        lastHash.ifPresent(update::setPrevHash);

        Converters
                .toObservable(gateway.updateMember(UpdateMemberRequest.newBuilder()
                        .setUpdate(update)
                        .setUpdateSignature(Signature.newBuilder()
                                .setMemberId(memberId)
                                .setKeyId(signer.getKeyId())
                                .setSignature(signer.sign(update.build())))
                        .build()))
                .subscribe(
                        response -> {
                            if (keyIndex == keys.size() - 1) {
                                callback.onSuccess(response.getMember());
                            } else {
                                addAllKeys(
                                        memberId,
                                        keys,
                                        keyIndex + 1,
                                        Optional.of(response.getMember().getLastHash()),
                                        signer,
                                        callback);
                            }
                        },
                        callback::onError);
    }
}
