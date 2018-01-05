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

package io.token.security;

import io.token.proto.common.security.SecurityProtos.Key;
import io.token.security.crypto.Crypto;
import io.token.security.crypto.CryptoRegistry;
import io.token.security.crypto.CryptoType;
import io.token.security.keystore.SecretKeyPair;

import java.security.KeyPair;

/**
 * Token implementation of the {@link io.token.security.CryptoEngine}. The keys are persisted
 * in the provided storage
 */
public final class AKSCryptoEngine implements io.token.security.CryptoEngine {
    private static final CryptoType CRYPTO_TYPE = CryptoType.EDDSA;
    private static final Key.Algorithm KEY_ALGORITHM = Key.Algorithm.ED25519;

    private final String memberId;
    private final io.token.security.KeyStore keyStore;
    private final Crypto crypto;

    /**
     * Creates an instance.
     *
     * @param memberId member ID
     * @param keyStore key store
     */
    public AKSCryptoEngine(String memberId, io.token.security.KeyStore keyStore) {
        this.memberId = memberId;
        this.keyStore = keyStore;
        this.crypto = CryptoRegistry
                .getInstance()
                .cryptoFor(CRYPTO_TYPE);
        System.out.println("USING AKS ....");
    }

    @Override
    public Key generateKey(Key.Level keyLevel) {
        SecretKeyPair keyPair = SecretKeyPair.create(CRYPTO_TYPE);
        io.token.security.SecretKey key = io.token.security.SecretKey.create(
                keyPair.id(),
                keyLevel,
                new KeyPair(keyPair.publicKey(), keyPair.privateKey()));
        keyStore.put(memberId, key);
        return Key.newBuilder()
                .setId(key.getId())
                .setAlgorithm(KEY_ALGORITHM)
                .setLevel(keyLevel)
                .setPublicKey(crypto.serialize(key.getPublicKey()))
                .build();
    }

    @Override
    public Signer createSigner(Key.Level keyLevel) {
        io.token.security.SecretKey key = keyStore.getByLevel(memberId, keyLevel);
        return crypto.signer(key.getId(), key.getPrivateKey());
    }

    @Override
    public Verifier createVerifier(String keyId) {
        io.token.security.SecretKey key = keyStore.getById(memberId, keyId);
        return crypto.verifier(key.getPublicKey());
    }
}
