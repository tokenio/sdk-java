package io.token;

import io.token.proto.common.member.MemberProtos;
import io.token.rpc.Client;
import io.token.security.SecretKey;
import io.token.util.codec.ByteEncoding;
import rx.Observable;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

/**
 * Represents a Member in the Token system. Each member has an active secret
 * and public key pair that is used to perform authentication.
 */
public final class Member {
    private final SecretKey key;
    private final Client client;
    private final MemberProtos.Member.Builder member;

    /**
     * @param member internal member representation, fetched from server
     * @param key secret/public key pair
     * @param client RPC client used to perform operations against the server
     */
    Member(MemberProtos.Member member, SecretKey key, Client client) {
        this.key = key;
        this.client = client;
        this.member = member.toBuilder();
    }

    /**
     * @return a unique ID that identifies the member in the Token system
     */
    public String getMemberId() {
        return member.getId();
    }

    /**
     * @return secret/public keys associated with this member instance
     */
    public SecretKey getKey() {
        return key;
    }

    /**
     * @return list of aliases owned by the member
     */
    public List<String> getAliases() {
       return member.getAliasesList();
    }

    /**
     * @return list of public keys that are approved for this member
     */
    public List<byte[]> getPublicKeys() {
        return member.getKeysBuilderList()
                .stream()
                .map(k -> ByteEncoding.parse(k.getPublicKey()))
                .collect(toList());
    }

    /**
     * Adds a new alias for the member.
     *
     * @param alias alias, e.g. 'john', must be unique
     */
    public void addAlias(String alias) {
        addAliasAsync(alias).toBlocking().single();
    }

    /**
     * Adds a new alias for the member.
     *
     * @param alias alias, e.g. 'john', must be unique
     */
    public Observable<Void> addAliasAsync(String alias) {
        return client
                .addAlias(member.build(), alias)
                .map(m -> {
                    member.clear().mergeFrom(m);
                    return null;
                });
    }

    /**
     * Removes an alias for the member.
     *
     * @param alias alias, e.g. 'john'
     */
    public void removeAlias(String alias) {
        removeAliasAsync(alias).toBlocking().single();
    }

    /**
     * Removes an alias for the member.
     *
     * @param alias alias, e.g. 'john'
     */
    public Observable<Void> removeAliasAsync(String alias) {
        return client
                .removeAlias(member.build(), alias)
                .map(m -> {
                    member.clear().mergeFrom(m);
                    return null;
                });
    }

    /**
     * Approves a public key owned by this member. The key is added to the list
     * of valid keys for the member.
     *
     * @param publicKey public key to add to the approved list
     * @param level key security level
     */
    public void approveKey(byte[] publicKey, int level) {
        approveKeyAsync(publicKey, level).toBlocking().single();
    }

    /**
     * Approves a public key owned by this member. The key is added to the list
     * of valid keys for the member.
     *
     * @param publicKey public key to add to the approved list
     * @param level key security level
     */
    public Observable<Void> approveKeyAsync(byte[] publicKey, int level) {
        return client
                .addKey(member.build(), level, publicKey)
                .map(m -> {
                    member.clear().mergeFrom(m);
                    return null;
                });
    }

    /**
     * Removes a public key owned by this member.
     *
     * @param keyId key ID of the key to remove
     */
    public void removeKey(String keyId) {
        removeKeyAsync(keyId).toBlocking().single();
    }

    /**
     * Removes a public key owned by this member.
     *
     * @param keyId key ID of the key to remove
     */
    public Observable<Void> removeKeyAsync(String keyId) {
        return client
                .removeKey(member.build(), keyId)
                .map(m -> {
                    member.clear().mergeFrom(m);
                    return null;
                });
    }

    /**
     * TODO(alexey): This will in the future lookup an existing account or link one.
     */
    public Account account() {
        return new Account(this, client);
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }
}
