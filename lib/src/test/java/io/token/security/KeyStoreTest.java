package io.token.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.token.proto.common.security.SecurityProtos.Key.Level;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Test;

abstract class KeyStoreTest {

    abstract KeyStore createKeyStore();

    @Test
    public void putThenGet() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        KeyPair keyPair = keyGen.genKeyPair();

        KeyStore store = createKeyStore();

        SecretKey laptop = SecretKey.create("laptop", Level.STANDARD, keyPair);
        store.put("steve", laptop);

        assertThat(store.getById("steve", "laptop")).isEqualTo(laptop);
        assertThat(store.getByLevel("steve", Level.STANDARD)).isEqualTo(laptop);

        // Replace key.
        laptop = SecretKey.create("laptop", Level.LOW, keyPair);
        store.put("steve", laptop);
        assertThat(store.getById("steve", "laptop")).isEqualTo(laptop);
        assertThat(store.getByLevel("steve", Level.LOW)).isEqualTo(laptop);

        // Add another key for the member.
        SecretKey phone = SecretKey.create("phone", Level.PRIVILEGED, keyPair);
        store.put("steve", phone);
        assertThat(store.getById("steve", "phone")).isEqualTo(phone);
        assertThat(store.getById("steve", "laptop")).isEqualTo(laptop);

        assertThat(store.listKeys("steve").size()).isEqualTo(2);
        store.deleteKeys("steve");


        assertThat(store.listKeys("steve").size()).isEqualTo(0);
    }

    @Test
    public void getAbsent() {
        final KeyStore store = createKeyStore();

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(new ThrowingCallable() {
                    @Override
                    public void call() throws IllegalArgumentException {
                        store.getById("steve", "laptop");
                    }
                });

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(new ThrowingCallable() {
                    @Override
                    public void call() throws IllegalArgumentException {
                        store.getByLevel("steve", Level.STANDARD);
                    }
                });
    }
}
