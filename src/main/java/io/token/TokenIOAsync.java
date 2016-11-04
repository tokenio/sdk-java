package io.token;

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
 * <p>
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
        SecretKey key = Crypto.generateSecretKey();
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);

        return unauthenticated
                .createMemberId()
                .flatMap(memberId -> unauthenticated.addFirstKey(memberId, key))
                .flatMap(member -> {
                    Client client = ClientFactory.authenticated(channel, member.getId(), null, key);
                    return client
                            .addUsername(member, username)
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
     * Logs in an existing member to the system, using an username
     *
     * @param username username
     * @param key secret/public key pair to use
     * @return logged in member
     */
    public Observable<MemberAsync> loginWithUsername(String username, SecretKey key) {
        Client client = ClientFactory.authenticated(channel, null, username, key);
        return client
                .getMember()
                .map(member -> new MemberAsync(member, key, client));
    }

    /**
     * Notifies to link an account
     *
     * @param username username to notify
     * @param bankId bank ID to link
     * @param bankName bank name to link
     * @param accountLinkPayloads a list of account payloads to be linked
     * @return nothing
     */
    public Observable<Void> notifyLinkAccounts(
            String username,
            String bankId,
            String bankName,
            List<String> accountLinkPayloads) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated
                .notifyLinkAccounts(username, bankId, bankName, accountLinkPayloads);
    }

    /**
     * Notifies to add a key
     *
     * @param username username to notify
     * @param publicKey public key to add
     * @return nothing
     */
    public Observable<Void> notifyAddKey(String username, byte[] publicKey, String name) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated
                .notifyAddKey(username, publicKey, name);
    }

    /**
     * Notifies to link accounts and add a key
     *
     * @param username username to notify
     * @param bankId bank ID to link
     * @param bankName bank name to link
     * @param accountLinkPayloads a list of account payloads to be linked
     * @param publicKey public key to add
     * @return nothing
     */
    public Observable<Void> notifyLinkAccountsAndAddKey(
            String username,
            String bankId,
            String bankName,
            List<String> accountLinkPayloads,
            byte[] publicKey,
            String name) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated
                .notifyLinkAccountsAndAddKey(
                        username,
                        bankId,
                        bankName,
                        accountLinkPayloads,
                        publicKey,
                        name);
    }
}
