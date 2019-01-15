/**
 * Copyright (c) 2017 Token, Inc.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.token.user.security;

import io.token.proto.common.security.SecurityProtos.Key;
import io.token.security.Signer;
import io.token.security.Verifier;
import io.token.security.crypto.Crypto;
import io.token.security.crypto.CryptoRegistry;
import io.token.security.crypto.CryptoType;
import io.token.security.keystore.SecretKeyPair;

import java.security.KeyPair;
import java.util.LinkedList;
import java.util.List;

/**
 * Token implementation of the {@link io.token.user.security.CryptoEngine}. The keys are persisted
 * in the provided storage.
 */
public final class TokenCryptoEngine implements CryptoEngine {
    private static final CryptoType CRYPTO_TYPE = CryptoType.EDDSA;
    private static final Key.Algorithm KEY_ALGORITHM = Key.Algorithm.ED25519;

    private final String memberId;
    private final KeyStore keyStore;
    private final Crypto crypto;

    /**
     * Creates an instance.
     *
     * @param memberId member ID
     * @param keyStore key store
     */
    public TokenCryptoEngine(String memberId, KeyStore keyStore) {
        this.memberId = memberId;
        this.keyStore = keyStore;
        this.crypto = CryptoRegistry
                .getInstance()
                .cryptoFor(CRYPTO_TYPE);
    }

    @Override
    public Key generateKey(Key.Level keyLevel) {
        SecretKeyPair keyPair = SecretKeyPair.create(CRYPTO_TYPE);
        SecretKey key = SecretKey.create(
                keyPair.id(),
                keyLevel,
                new KeyPair(keyPair.publicKey(), keyPair.privateKey()));
        keyStore.put(memberId, key);
        return toPublicKey(key);
    }

    @Override
    public Key generateKey(Key.Level keyLevel, long expiresAtMs) {
        SecretKeyPair keyPair = SecretKeyPair.create(CRYPTO_TYPE);
        SecretKey key = SecretKey.create(
                keyPair.id(),
                keyLevel,
                new KeyPair(keyPair.publicKey(), keyPair.privateKey()),
                expiresAtMs);
        keyStore.put(memberId, key);
        return toPublicKey(key);
    }

    @Override
    public Signer createSigner(Key.Level keyLevel) {
        SecretKey key = keyStore.getByLevel(memberId, keyLevel);
        return crypto.signer(key.getId(), key.getPrivateKey());
    }

    @Override
    public Verifier createVerifier(String keyId) {
        SecretKey key = keyStore.getById(memberId, keyId);
        return crypto.verifier(key.getPublicKey());
    }

    @Override
    public List<Key> getPublicKeys() {
        List<Key> publicKeys = new LinkedList<>();
        List<SecretKey> secretKeys = keyStore.listKeys(memberId);
        for (SecretKey secretKey : secretKeys) {
            publicKeys.add(toPublicKey(secretKey));
        }
        return publicKeys;
    }

    private Key toPublicKey(SecretKey secretKey) {
        long expiresAtMs = secretKey.getExpiresAtMs() == null
                ? 0
                : secretKey.getExpiresAtMs();
        return Key.newBuilder()
                .setId(secretKey.getId())
                .setAlgorithm(KEY_ALGORITHM)
                .setLevel(secretKey.getLevel())
                .setPublicKey(crypto.serialize(secretKey.getPublicKey()))
                .setExpiresAtMs(expiresAtMs)
                .build();
    }

    @Override
    public void deleteKeys() {
        keyStore.deleteKeys(memberId);
    }
}
