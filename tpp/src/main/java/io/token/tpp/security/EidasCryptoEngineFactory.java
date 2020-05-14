package io.token.tpp.security;

import io.token.security.CryptoEngine;
import io.token.security.CryptoEngineFactory;
import io.token.security.TokenCryptoEngine;
import io.token.security.crypto.CryptoType;

public class EidasCryptoEngineFactory implements CryptoEngineFactory {
    private final EidasKeyStore keyStore;

    public EidasCryptoEngineFactory(EidasKeyStore keyStore) {
        this.keyStore = keyStore;
    }

    @Override
    public CryptoEngine create(String memberId) {
        return new TokenCryptoEngine(memberId, keyStore, CryptoType.RS256);
    }

}
