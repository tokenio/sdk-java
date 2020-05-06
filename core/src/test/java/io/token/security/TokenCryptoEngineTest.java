package io.token.security;

import static io.token.proto.common.security.SecurityProtos.Key.Algorithm.ED25519;
import static io.token.proto.common.security.SecurityProtos.Key.Algorithm.RS256;
import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.util.TimeUtil.daysAfter;
import static io.token.util.TimeUtil.daysToMs;
import static io.token.util.Util.generateNonce;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.token.exceptions.KeyNotFoundException;
import io.token.proto.common.security.SecurityProtos;
import io.token.security.crypto.CryptoType;
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
    public void createSigner_byKeyId() {
        KeyStore keyStore = new InMemoryKeyStore();
        CryptoEngine cryptoEngine = new TokenCryptoEngine("member-id", keyStore);

        SecurityProtos.Key k1 = cryptoEngine.generateKey(STANDARD);
        SecurityProtos.Key k2 = cryptoEngine.generateKey(STANDARD);

        assertThat(cryptoEngine.createSigner(k1.getId()).getKeyId()).isEqualTo(k1.getId());
        assertThat(cryptoEngine.createSigner(k2.getId()).getKeyId()).isEqualTo(k2.getId());
    }

    @Test
    public void createSigner_forMinLeve() {
        KeyStore keyStore = new InMemoryKeyStore();
        CryptoEngine cryptoEngine = new TokenCryptoEngine("member-id", keyStore);

        assertThatExceptionOfType(KeyNotFoundException.class)
                .isThrownBy(() -> cryptoEngine.createSignerForLevelAtLeast(LOW));

        SecurityProtos.Key privileged = cryptoEngine.generateKey(PRIVILEGED);
        assertThat(cryptoEngine.createSignerForLevelAtLeast(LOW).getKeyId())
                .isEqualTo(privileged.getId());
        assertThat(cryptoEngine.createSignerForLevelAtLeast(STANDARD).getKeyId())
                .isEqualTo(privileged.getId());
        assertThat(cryptoEngine.createSignerForLevelAtLeast(PRIVILEGED).getKeyId())
                .isEqualTo(privileged.getId());

        SecurityProtos.Key low = cryptoEngine.generateKey(STANDARD);
        assertThat(cryptoEngine.createSignerForLevelAtLeast(LOW).getKeyId()).isEqualTo(low.getId());
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

    @Test
    public void createCryptoEngine_cryptoType() {
        KeyStore keyStore = new InMemoryKeyStore();
        CryptoEngine cryptoEngineDefault = new TokenCryptoEngine(generateNonce(), keyStore);
        cryptoEngineDefault.generateKey(LOW);
        // default crypto type
        assertThat(cryptoEngineDefault.getPublicKeys().get(0).getAlgorithm()).isEqualTo(ED25519);

        // another crypto type
        CryptoEngine cryptoEngineRsa = new TokenCryptoEngine(
                generateNonce(),
                keyStore,
                CryptoType.RS256);
        cryptoEngineRsa.generateKey(LOW);
        assertThat(cryptoEngineRsa.getPublicKeys().get(0).getAlgorithm()).isEqualTo(RS256);
    }
}
