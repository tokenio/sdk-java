package io.token.security;

public class InMemoryKeyStoreTest extends KeyStoreTest {
    @Override
    KeyStore createKeyStore() {
        return new InMemoryKeyStore();
    }
}
