package io.token.security;

import io.token.proto.common.security.SecurityProtos;

/**
 * Provides key storage primitives.
 */
public interface KeyStore {
    /**
     * Puts a specified key into the storage.
     *
     * @param memberId member ID
     * @param key key to put into the storage
     */
    void put(String memberId, SecretKey key);

    /**
     * Gets a key by its level.
     *
     * @param memberId member ID
     * @param keyLevel {@link SecurityProtos.Key.Level} of the key to get
     * @return secret key
     */
    SecretKey getByLevel(String memberId, SecurityProtos.Key.Level keyLevel);

    /**
     * Gets a key by its ID.
     *
     * @param memberId member ID
     * @param keyId key ID to get
     * @return secret key
     */
    SecretKey getById(String memberId, String keyId);
}
