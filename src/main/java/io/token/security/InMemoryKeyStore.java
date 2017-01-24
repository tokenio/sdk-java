package io.token.security;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import io.token.proto.common.security.SecurityProtos;

/**
 * In memory implementation of the {@link KeyStore}. Used for testing.
 */
public final class InMemoryKeyStore implements KeyStore {
    private final Multimap<String, SecretKey> keys = ArrayListMultimap.create();

    @Override
    public void put(String memberId, SecretKey key) {
        keys.put(memberId, key);
    }

    @Override
    public SecretKey getByLevel(String memberId, SecurityProtos.Key.Level keyLevel) {
        return keys.get(memberId)
                .stream()
                .filter(k -> k.getLevel().equals(keyLevel))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Key not found for level: " + keyLevel));
    }

    @Override
    public SecretKey getById(String memberId, String keyId) {
        return keys.get(memberId)
                .stream()
                .filter(k -> k.getId().equals(keyId))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Key not found for id: " + keyId));
    }
}
