package io.token.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.token.proto.common.security.SecurityProtos.Key.Level;
import io.token.user.security.KeyStore;
import io.token.user.security.SecretKey;
import io.token.user.security.UnsecuredFileSystemKeyStore;
import io.token.user.util.Clock;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class UnsecuredFileSystemKeyStoreTest extends KeyStoreTest {
    @Rule public TemporaryFolder tempDir = new TemporaryFolder();

    @Override
    KeyStore createKeyStore(Clock clock) {
        return new io.token.user.security.UnsecuredFileSystemKeyStore(tempDir.getRoot(), clock);
    }

    @Test
    public void testDelete() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        KeyPair keyPair = keyGen.genKeyPair();

        io.token.user.security.UnsecuredFileSystemKeyStore store = new UnsecuredFileSystemKeyStore(tempDir.getRoot());

        io.token.user.security.SecretKey laptop = SecretKey.create("laptop", Level.STANDARD, keyPair);
        store.put("steve", laptop);
        assertThat(store.listKeys("steve").size()).isEqualTo(1);

        store.deleteKeys("steve");
        assertThat(store.listKeys("steve").size()).isEqualTo(0);
    }
}
