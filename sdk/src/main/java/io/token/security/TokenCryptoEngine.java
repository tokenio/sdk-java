package io.token.security;

import static io.token.security.keystore.SecurityConfigHelper.keyIdFor;

import io.token.proto.common.security.SecurityProtos.Key;
import io.token.security.crypto.Crypto;
import io.token.security.crypto.CryptoRegistry;
import io.token.security.crypto.CryptoType;

import java.security.KeyPair;

/**
 * Token implementation of the {@link CryptoEngine}. The keys are persisted
 * in the provided storage
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
        KeyPair keyPair = crypto.generateKeyPair();
        String id = keyIdFor(crypto.serialize(keyPair.getPublic()));
        SecretKey key = new SecretKey(id, keyLevel, keyPair);
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
        SecretKey key = keyStore.getByLevel(memberId, keyLevel);
        return crypto.signer(key.getId(), key.getPrivateKey());
    }

    @Override
    public Verifier createVerifier(String keyId) {
        SecretKey key = keyStore.getById(memberId, keyId);
        return crypto.verifier(key.getPublicKey());
    }
}
