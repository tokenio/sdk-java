package io.token.security;

import io.token.proto.common.security.SecurityProtos.Key;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

final class SecretKey {
    private final String id;
    private final Key.Level level;
    private final KeyPair keyPair;

    public SecretKey(String id, Key.Level level, KeyPair keyPair) {
        this.id = id;
        this.level = level;
        this.keyPair = keyPair;
    }

    public String getId() {
        return id;
    }

    public Key.Level getLevel() {
        return level;
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }
}
