package io.token.security;

/**
 * Creates {@link CryptoEngine} instances bound to a given member id.
 */
public interface CryptoEngineFactory {
    /**
     * Creates a new {@link CryptoEngine} for the given member.
     *
     * @param memberId member id
     * @return crypto engine instance
     */
    CryptoEngine create(String memberId);
}
