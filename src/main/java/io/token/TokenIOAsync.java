package io.token;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.token.rpc.Client;
import io.token.rpc.ClientFactory;
import io.token.rpc.UnauthenticatedClient;
import io.token.security.Crypto;
import io.token.security.SecretKey;
import rx.Observable;

import java.util.List;

/**
 * Use this class to create to create a new member using {@link #createMember}
 * method or login an existing member using {@link #login}.
 *
 * <p>
 * The class provides async API with {@link TokenIO} providing a synchronous
 * version. {@link TokenIO} instance can be obtained by calling {@link #sync}
 * method.
 * </p>
 */
public final class TokenIOAsync {
    private final ManagedChannel channel;

    TokenIOAsync(ManagedChannel channel) {
        this.channel = channel;
    }

    /**
     * @return synchronous version of the account API
     */
    public TokenIO sync() {
        return new TokenIO(this);
    }

    /**
     * Creates a new Token member with a pair of auto generated keys and the
     * given alias.
     *
     * @param alias member alias to use, must be unique
     * @return newly created member
     */
    public Observable<MemberAsync> createMember(String alias) {
        SecretKey key = Crypto.generateSecretKey();
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);

        return unauthenticated
                .createMemberId()
                .flatMap(memberId -> unauthenticated.addFirstKey(memberId, key))
                .flatMap(member -> {
                    Client client = ClientFactory.authenticated(channel, member.getId(), null, key);
                    return client
                            .addAlias(member, alias)
                            .map(m -> new MemberAsync(m, key, client));
                });
    }

    /**
     * Logs in an existing member to the system.
     *
     * @param memberId member id
     * @param key secret/public key pair to use
     * @return logged in member
     */
    public Observable<MemberAsync> login(String memberId, SecretKey key) {
        Client client = ClientFactory.authenticated(channel, memberId, null, key);
        return client
                .getMember()
                .map(member -> new MemberAsync(member, key, client));
    }

    /**
     * Logs in an existing member to the system, using an alias
     *
     * @param alias alias
     * @param key secret/public key pair to use
     * @return logged in member
     */
    public Observable<MemberAsync> loginWithAlias(String alias, SecretKey key) {
        Client client = ClientFactory.authenticated(channel, null, alias, key);
        return client
                .getMember()
                .map(member -> new MemberAsync(member, key, client));
    }

    /**
     * Notifies to link an account
     *
     * @param alias: alias to notify
     * @param bankId: bank to link
     * @param accountLinkPayload: payload generated by bank
     * @return nothing
     */
    public Observable<Void> notifyLinkAccounts(String alias, String bankId,
                                               byte[] accountLinkPayload) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated
                .notifyLinkAccounts(alias, bankId, accountLinkPayload);
    }

    /**
     * Notifies to add a key
     *
     * @param alias alias to notify
     * @param publicKey public key to add
     * @param tags tags for this key
     * @return nothing
     */
    public Observable<Void> notifyAddKey(String alias, byte[] publicKey,
                                               List<String> tags) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated
                .notifyAddKey(alias, publicKey, tags);
    }

    /**
     * Notifies to link accounts and add a key
     *
     * @param alias alias to notify
     * @param bankId bankId to link
     * @param accountLinkPayload payload generated by bank
     * @param publicKey public key to add
     * @param tags tags for the public key
     * @return nothing
     */
    public Observable<Void> notifyLinkAccountsAndAddKey(String alias, String bankId,
                                                        byte[] accountLinkPayload,
                                                        byte[] publicKey, List<String> tags) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated
                .notifyLinkAccountsAndAddKey(alias, bankId, accountLinkPayload, publicKey, tags);
    }
}
