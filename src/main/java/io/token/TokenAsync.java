package io.token;

import static io.grpc.Status.NOT_FOUND;
import static java.util.Arrays.asList;

import com.google.common.collect.ImmutableList;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.security.SecurityProtos.SealedMessage;
import io.token.rpc.Client;
import io.token.rpc.ClientFactory;
import io.token.rpc.UnauthenticatedClient;
import io.token.security.CryptoEngine;
import io.token.security.CryptoEngineFactory;
import io.token.security.Signer;

import java.util.List;
import rx.Observable;

/**
 * Use this class to create to create a new member using {@link #createMember}
 * method or login an existing member using {@link #login}.
 * <p></p>The class provides async API with {@link Token} providing a synchronous
 * version. {@link Token} instance can be obtained by calling {@link #sync}
 * method.
 * </p>
 */
public final class TokenAsync {
    private final ManagedChannel channel;
    private final CryptoEngineFactory cryptoFactory;

    TokenAsync(ManagedChannel channel, CryptoEngineFactory cryptoFactory) {
        this.channel = channel;
        this.cryptoFactory = cryptoFactory;
    }

    /**
     * Returns a sync version of the API.
     *
     * @return synchronous version of the account API
     */
    public Token sync() {
        return new Token(this);
    }

    /**
     * Checks if a given username already exists.
     *
     * @param username username to check
     * @return {@code true} if username exists, {@code false} otherwise
     */
    public Observable<Boolean> usernameExists(String username) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.usernameExists(username);
    }

    /**
     * Creates a new Token member with a pair of auto generated keys and the
     * given username.
     *
     * @param username member username to use, must be unique
     * @return newly created member
     */
    public Observable<MemberAsync> createMember(String username) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated
                .createMemberId()
                .flatMap(memberId -> {
                    CryptoEngine crypto = cryptoFactory.create(memberId);
                    List<Key> keys = asList(
                            crypto.generateKey(Key.Level.PRIVILEGED),
                            crypto.generateKey(Key.Level.STANDARD),
                            crypto.generateKey(Key.Level.LOW));
                    Signer signer = crypto.createSigner(Key.Level.PRIVILEGED);
                    return unauthenticated.addKeys(memberId, keys, signer);
                })
                .flatMap(member -> {
                    CryptoEngine crypto = cryptoFactory.create(member.getId());
                    Client client = ClientFactory.authenticated(
                            channel,
                            member.getId(),
                            crypto);
                    return client
                            .addUsernames(member, ImmutableList.of(username))
                            .map(m -> new MemberAsync(m, client));
                });
    }

    /**
     * Provisions a new device for an existing user. The call generates a set
     * of keys that are returned back. The keys need to be approved by an
     * existing device/keys.
     *
     * @param username member id to provision the device for
     * @return device information
     */
    public Observable<DeviceInfo> provisionDevice(String username) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated
                .getMemberId(username)
                .map(memberId -> {
                    if (memberId == null) {
                        throw new StatusRuntimeException(NOT_FOUND);
                    }

                    CryptoEngine crypto = cryptoFactory.create(memberId);
                    return new DeviceInfo(
                            memberId,
                            asList(
                                    crypto.generateKey(Key.Level.PRIVILEGED),
                                    crypto.generateKey(Key.Level.STANDARD),
                                    crypto.generateKey(Key.Level.LOW)));
                });
    }

    /**
     * Logs in an existing member to the system.
     *
     * @param memberId member id
     * @return logged in member
     */
    public Observable<MemberAsync> login(String memberId) {
        CryptoEngine crypto = cryptoFactory.create(memberId);
        Client client = ClientFactory.authenticated(channel, memberId, crypto);
        return client
                .getMember()
                .map(member -> new MemberAsync(member, client));
    }

    /**
     * Notifies to link an account.
     *
     * @param username username to notify
     * @param bankId bank ID to link
     * @param bankName bank name to link
     * @param accountLinkPayloads a list of account payloads to be linked
     * @return status of the notification
     */
    public Observable<NotifyStatus> notifyLinkAccounts(
            String username,
            String bankId,
            String bankName,
            List<SealedMessage> accountLinkPayloads) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated
                .notifyLinkAccounts(username, bankId, bankName, accountLinkPayloads);
    }

    /**
     * Notifies to add a key.
     *
     * @param username username to notify
     * @param name device/client name, e.g. iPhone, Chrome Browser, etc
     * @param key key that needs an approval
     * @return status of the notification
     */
    public Observable<NotifyStatus> notifyAddKey(
            String username,
            String name,
            Key key) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.notifyAddKey(username, name, key);
    }

    /**
     * Notifies to link accounts and add a key.
     *
     * @param username username to notify
     * @param bankId bank ID to link
     * @param bankName bank name to link
     * @param accountLinkPayloads a list of account payloads to be linked
     * @param name device/client name, e.g. iPhone, Chrome Browser, etc
     * @param key the that needs an approval
     * @return status of the notification
     */
    public Observable<NotifyStatus> notifyLinkAccountsAndAddKey(
            String username,
            String bankId,
            String bankName,
            List<SealedMessage> accountLinkPayloads,
            String name,
            Key key) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.notifyLinkAccountsAndAddKey(
                username,
                bankId,
                bankName,
                accountLinkPayloads,
                name,
                key);
    }
}
