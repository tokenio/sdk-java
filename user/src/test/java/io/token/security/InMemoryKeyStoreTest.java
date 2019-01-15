package io.token.security;

import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.user.security.InMemoryKeyStore;
import io.token.user.security.KeyStore;
import io.token.user.security.SecretKey;
import io.token.user.util.Clock;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import org.junit.Test;

public class InMemoryKeyStoreTest extends KeyStoreTest {
    @Override
    KeyStore createKeyStore(Clock clock) {
        return new io.token.user.security.InMemoryKeyStore(clock);
    }

    @Test
    public void testDelete() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        KeyPair keyPair = keyGen.genKeyPair();

        io.token.user.security.InMemoryKeyStore store = new InMemoryKeyStore();

        io.token.user.security.SecretKey laptop = SecretKey.create("laptop", STANDARD, keyPair);
        store.put("steve", laptop);
        assertThat(store.listKeys("steve").size()).isEqualTo(1);

        store.deleteKeys("steve");
        assertThat(store.listKeys("steve").size()).isEqualTo(0);
    }
}
