package io.token.security;

import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.util.Clock;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import org.junit.Test;

public class InMemoryKeyStoreTest extends KeyStoreTest {
    @Override
    KeyStore createKeyStore(Clock clock) {
        return new InMemoryKeyStore(clock);
    }

    @Test
    public void testDelete() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        KeyPair keyPair = keyGen.genKeyPair();

        InMemoryKeyStore store = new InMemoryKeyStore();

        SecretKey laptop = SecretKey.create("laptop", STANDARD, keyPair);
        store.put("steve", laptop);
        assertThat(store.listKeys("steve").size()).isEqualTo(1);

        store.deleteKeys("steve");
        assertThat(store.listKeys("steve").size()).isEqualTo(0);
    }
}
