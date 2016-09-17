package io.token;

import io.grpc.ManagedChannel;
import io.token.rpc.Client;
import io.token.rpc.ClientFactory;
import io.token.rpc.UnauthenticatedClient;
import io.token.security.Crypto;
import io.token.security.SecretKey;
import rx.Observable;

/**
 * Main entry point to the Token SDK. Use this class to create an instance of
 * the {@link TokenIOAsync} and then use {@link #createMember} to create new member
 * or {@link #login} to login an existing member.
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
                    Client client = ClientFactory.authenticated(channel, member.getId(), key);
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
        Client client = ClientFactory.authenticated(channel, memberId, key);
        return client
                .getMember()
                .map(member -> new MemberAsync(member, key, client));
    }
}
