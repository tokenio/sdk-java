package io.token.security;

import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.util.TimeUtil.daysAfter;
import static io.token.util.TimeUtil.daysToMs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.token.proto.common.security.SecurityProtos;
import io.token.util.TestClock;
import org.assertj.core.api.ThrowableAssert;
import org.junit.Test;

public class TokenCryptoEngineTest {

    @Test
    public void createSigner_usesNonExpiredKey() {
        TestClock clock = new TestClock();
        KeyStore keyStore = new InMemoryKeyStore(clock);
        CryptoEngine cryptoEngine = new TokenCryptoEngine("member-id", keyStore);
        long now = clock.getTime();

        cryptoEngine.generateKey(STANDARD, daysAfter(now, 1));
        SecurityProtos.Key validKey = cryptoEngine.generateKey(STANDARD, daysAfter(now, 3));
        clock.tick(daysToMs(2));
        assertThat(cryptoEngine.createSigner(STANDARD).getKeyId()).isEqualTo(validKey.getId());

        now = clock.getTime();
        SecurityProtos.Key validPrivilegedKey = cryptoEngine
                .generateKey(PRIVILEGED, daysAfter(now, 3));
        cryptoEngine.generateKey(PRIVILEGED, daysAfter(now, 1));
        clock.tick(daysToMs(2));
        assertThat(cryptoEngine.createSigner(PRIVILEGED).getKeyId())
                .isEqualTo(validPrivilegedKey.getId());
    }

    @Test
    public void createVerifier_enforcesNonExpired() {
        TestClock clock = new TestClock();
        KeyStore keyStore = new InMemoryKeyStore(clock);
        final CryptoEngine cryptoEngine = new TokenCryptoEngine("member-id", keyStore);
        long now = clock.getTime();

        final SecurityProtos.Key expiredKey = cryptoEngine.generateKey(STANDARD, daysAfter(now, 1));
        clock.tick(daysToMs(2));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                new ThrowableAssert.ThrowingCallable() {
                    @Override
                    public void call() {
                        cryptoEngine.createVerifier(expiredKey.getId());
                    }
                });
    }
}
