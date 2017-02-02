package io.token.security;

import io.token.proto.common.security.SecurityProtos.Key;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Encapsulates secret key data.
 */
final class SecretKey {
    private final String id;
    private final Key.Level level;
    private final KeyPair keyPair;

    /**
     * Creates an instance.
     *
     * @param id key ID
     * @param level key level
     * @param keyPair secret key pair
     */
    public SecretKey(String id, Key.Level level, KeyPair keyPair) {
        this.id = id;
        this.level = level;
        this.keyPair = keyPair;
    }

    /**
     * Gets key ID.
     *
     * @return key ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets key level.
     *
     * @return key level
     */
    public Key.Level getLevel() {
        return level;
    }

    /**
     * Gets public key.
     *
     * @return public key
     */
    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    /**
     * Gets private key.
     *
     * @return private key
     */
    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }
}
