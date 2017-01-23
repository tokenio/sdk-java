package io.token.security;

import io.token.proto.common.security.SecurityProtos;

public interface KeyStore {
    void put(String memberId, SecretKey key);

    SecretKey getByLevel(String memberId, SecurityProtos.Key.Level keyLevel);

    SecretKey getById(String memberId, String keyId);
}
