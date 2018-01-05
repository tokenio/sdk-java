package io.token.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.token.proto.common.security.SecurityProtos.Key.Level;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class UnsecuredFileSystemKeyStoreTest extends KeyStoreTest {
    @Rule public TemporaryFolder tempDir = new TemporaryFolder();

    @Override
    KeyStore createKeyStore() {
        return new UnsecuredFileSystemKeyStore(tempDir.getRoot());
    }

    @Test
    public void testDelete() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        KeyPair keyPair = keyGen.genKeyPair();

        UnsecuredFileSystemKeyStore store = new UnsecuredFileSystemKeyStore(tempDir.getRoot());

        SecretKey laptop = SecretKey.create("laptop", Level.STANDARD, keyPair);
        store.put("steve", laptop);
        assertThat(store.listKeys("steve").size()).isEqualTo(1);

        store.deleteKeys("steve");
        assertThat(store.listKeys("steve").size()).isEqualTo(0);
    }
}
