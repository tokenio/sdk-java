/**
 * Copyright (c) 2017 Token, Inc.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.token.security;

import android.content.Context;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import com.google.common.hash.Hashing;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.util.codec.ByteEncoding;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.ECGenParameterSpec;
import java.util.Calendar;
import java.util.Enumeration;
import javax.security.auth.x500.X500Principal;

/**
 * Android KeyStore implementation of the {@link CryptoEngine}. The keys are
 * persisted in the android KeyStore, stored in hardware if the device supports it.
 */
public final class AKSCryptoEngine implements CryptoEngine {
    private static final String KEY_NAME = "tokenapp_key";
    private static final Key.Algorithm KEY_ALGORITHM = Key.Algorithm.ECDSA_SHA256;
    private static final int AUTHENTICATION_DURATION_SECONDS = 5;

    private final String memberId;
    private final Context context;
    private final UserAuthenticationStore userAuthenticationStore;
    private final KeyStore keyStore;

    /**
     * Creates an instance.
     *
     * @param memberId member ID
     * @param context context, to draw UI
     */
    public AKSCryptoEngine(
            String memberId,
            Context context,
            UserAuthenticationStore userAuthenticationStore) {
        this.memberId = memberId;
        this.context = context;
        this.userAuthenticationStore = userAuthenticationStore;
        try {
            this.keyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Generates an ECDSA private key and returns the public key. The private key is stored in the
     * Android KeyStore, and will never leave the secure hardware, on phones that support it. Two
     * different classes are used to create the key, based on phone version.
     *
     * @param keyLevel key privilege level
     * @return public key
     */
    @Override
    public Key generateKey(Key.Level keyLevel) {
        KeyPairGenerator kpg;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // API 23 and higher, uses new API. This allows us to setUserAuthenticationRequired,
                // which validates that the user has recently authenticated, and this is checked by
                // they KeyStore, and trusted hardware, if it's available.
                KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                        getAlias(keyLevel),
                        KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                        .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                        .setDigests(KeyProperties.DIGEST_SHA256)
                        .setUserAuthenticationRequired(keyLevel != Key.Level.LOW)
                        .setUserAuthenticationValidityDurationSeconds(
                            AUTHENTICATION_DURATION_SECONDS);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // On Android N and above, we can invalidate the key if the user changes
                    // their biometrics
                    builder.setInvalidatedByBiometricEnrollment(true);
                }
                kpg = KeyPairGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");

                kpg.initialize(builder.build());
            } else {
                // Uses old method of generating keys.
                Calendar start = Calendar.getInstance();
                Calendar end = Calendar.getInstance();
                end.add(Calendar.YEAR, 256);
                KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
                        .setAlias(getAlias(keyLevel))
                        .setKeyType("EC")
                        .setKeySize(256)
                        .setSubject(new X500Principal("CN=myKey"))
                        .setStartDate(start.getTime())
                        .setEndDate(end.getTime())
                        .setSerialNumber(BigInteger.ONE)
                        .build();
                kpg = KeyPairGenerator.getInstance(
                        "RSA",
                        "AndroidKeyStore");
                kpg.initialize(spec);
            }
        } catch (NoSuchAlgorithmException | NoSuchProviderException |
                InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }

        KeyPair kp = kpg.generateKeyPair();
        String serializedPk = ByteEncoding.serialize(kp.getPublic().getEncoded());

        return Key.newBuilder()
                .setId(keyIdFor(serializedPk))
                .setAlgorithm(KEY_ALGORITHM)
                .setLevel(keyLevel)
                .setPublicKey(serializedPk)
                .build();
    }

    /**
     * Creates a signer object that uses the KeyStore to perform digital signatures.
     * A TokenAuthenticationException can be thrown while signing,
     * if the user has not authenticated recently.
     *
     * @param keyLevel level of the key to use
     * @return Sign
     */
    @Override
    public Signer createSigner(Key.Level keyLevel) {
        return new AKSSigner(
                getKeyFromKeyStore(keyLevel),
                keyLevel,
                userAuthenticationStore,
                AUTHENTICATION_DURATION_SECONDS);
    }

    /**
     * Creates a verifier object that verifies signatures made by the key in the KeyStore.
     *
     * @param keyId key id
     * @return Verifier
     */
    @Override
    public Verifier createVerifier(String keyId) {
        return new AKSVerifier(getKeyFromKeyStore(keyId));
    }

    /**
     * Gets a key Entry from the keystore, by the keyLevel.
     *
     * @param keyLevel keyLevel
     * @return Entry
     */
    private KeyStore.Entry getKeyFromKeyStore(Key.Level keyLevel) {
        try {
            keyStore.load(null);
            KeyStore.Entry entry = keyStore.getEntry(getAlias(keyLevel), null);
            if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
                throw new RuntimeException("Invalid private key");
            }
            return entry;
        } catch (GeneralSecurityException | IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Gets a key Entry from the keystore, by the keyId.
     *
     * @param keyId keyId
     * @return Entry
     */
    private KeyStore.Entry getKeyFromKeyStore(String keyId) {
        try {
            keyStore.load(null);
            Enumeration<String> aliases = keyStore.aliases();
            while(aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                byte[] publicKey = keyStore.getCertificate(alias).getPublicKey().getEncoded();
                if (keyIdFor(ByteEncoding.serialize(publicKey)).equals(keyId)) {
                    return keyStore.getEntry(alias, null);
                }
            }
        } catch (GeneralSecurityException | IOException exception) {
            throw new RuntimeException(exception);
        }
        throw new KeyNotFoundException(keyId);
    }

    /**
     * Gets the alias for a key of a certain level.
     *
     * @param keyLevel keyLevel
     * @return alias
     */
    private String getAlias(Key.Level keyLevel) {
        return KEY_NAME + "-" + memberId + "-" + keyLevel.toString();
    }

    /**
     * Calculates the keyId by hashing the key bytes.
     *
     * @param serializedKey key in String encoded format
     * @return keyId
     */
    public static String keyIdFor(String serializedKey) {
        byte[] encoded = ByteEncoding.parse(serializedKey);
        byte[] hash = Hashing.sha256().newHasher()
                .putBytes(encoded)
                .hash()
                .asBytes();
        return ByteEncoding.serialize(hash).substring(0, 16);
    }
}
