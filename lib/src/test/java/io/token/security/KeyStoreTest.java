package io.token.security;

import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.util.TimeUtil.daysAfter;
import static io.token.util.TimeUtil.daysToMs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.token.proto.common.security.SecurityProtos.Key.Level;
import io.token.util.Clock;
import io.token.util.TestClock;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.assertj.core.api.ThrowableAssert;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Test;

abstract class KeyStoreTest {

    abstract KeyStore createKeyStore(Clock clock);

    KeyStore createKeyStore() {
        return createKeyStore(new TestClock());
    }

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

    @Test
    public void getExpired() throws Exception {
        TestClock clock = new TestClock();
        final KeyStore store = createKeyStore(clock);
        long now = clock.getTime();

        SecretKey expired = SecretKey.create(
                "past",
                STANDARD,
                generateKeyPair(),
                daysAfter(now, 1));
        SecretKey future = SecretKey.create(
                "future",
                STANDARD,
                generateKeyPair(),
                daysAfter(now, 3));
        SecretKey noExpiration = SecretKey.create("eternal", STANDARD, generateKeyPair());

        store.put("steve", expired);
        store.put("steve", future);
        store.put("steve", noExpiration);

        clock.tick(daysToMs(2));

        assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                store.getById("steve", "past");
            }
        }).hasMessageContaining("has expired");
        assertThat(store.getById("steve", "eternal")).isEqualTo(noExpiration);
        assertThat(store.getById("steve", "future")).isEqualTo(future);

        List<SecretKey> keys = store.listKeys("steve");
        assertThat(keys).containsExactlyInAnyOrder(future, noExpiration);

        now = clock.getTime();
        SecretKey privilegedExpired = SecretKey.create(
                "privileged-past",
                PRIVILEGED,
                generateKeyPair(),
                daysAfter(now, 1));
        store.put("steve", privilegedExpired);
        clock.tick(daysToMs(2));
        assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                store.getByLevel("steve", PRIVILEGED);
            }
        });
    }

    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        return keyGen.genKeyPair();
    }
}
