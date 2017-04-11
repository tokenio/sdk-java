package io.token.security;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class UnsecuredFileSystemKeyStoreTest extends KeyStoreTest {
    @Rule public TemporaryFolder tempDir = new TemporaryFolder();

    @Override
    KeyStore createKeyStore() {
        return new UnsecuredFileSystemKeyStore(tempDir.getRoot());
    }
}
