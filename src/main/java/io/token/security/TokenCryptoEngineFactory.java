package io.token.security;

/**
 * Creates {@link CryptoEngine} instances bound to a given member id.
 * Uses a provided key store to persist keys.
 */
public class TokenCryptoEngineFactory implements CryptoEngineFactory {
    private final KeyStore keyStore;

    /**
     * Creates a new instance of the factory that uses supplied store
     * to persist the keys.
     *
     * @param keyStore key store
     */
    public TokenCryptoEngineFactory(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    /**
     * Creates a new {@link CryptoEngine} for the given member.
     *
     * @param memberId member id
     * @return crypto engine instance
     */
    @Override
    public CryptoEngine create(String memberId) {
        return new TokenCryptoEngine(memberId, keyStore);
    }
}
