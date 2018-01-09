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
import com.google.common.hash.Hashing;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.util.codec.ByteEncoding;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyPairGeneratorSpi;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.util.Calendar;
import java.util.Enumeration;
import javax.security.auth.x500.X500Principal;

/**
 * Android KeyStore implementation of the {@link io.token.security.CryptoEngine}. The keys are
 * persisted in the android KeyStore, stored in hardware if the device supports it.
 */
public final class AKSCryptoEngine implements io.token.security.CryptoEngine {
    private static final Key.Algorithm KEY_ALGORITHM = Key.Algorithm.ECDSA_SHA256;
    private static final int AUTHENTICATION_DURATION_SECONDS = 5*60;

    private static final String KEY_NAME = "tokenapp_key";
    private final String memberId;
    private final Context context;

    /**
     * Creates an instance.
     *
     * @param memberId member ID
     * @param context context, to draw UI
     */
    public AKSCryptoEngine(
            String memberId,
            Context context) {
        this.memberId = memberId;
        this.context = context;
    }

    @Override
    public Key generateKey(Key.Level keyLevel) {

        KeyPairGenerator kpg;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // API 23 and higher, use new API
                KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                        getAlias(keyLevel),
                        KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                        .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                        .setDigests(KeyProperties.DIGEST_SHA256)
                        .setUserAuthenticationRequired(keyLevel != Key.Level.LOW)
                        .setUserAuthenticationValidityDurationSeconds(
                                AUTHENTICATION_DURATION_SECONDS);
                kpg = KeyPairGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");

                kpg.initialize(builder.build());
            } else {
                // Use old method of generating keys
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
        System.out.println("PK: " + serializedPk);

        return Key.newBuilder()
                .setId(keyIdFor(serializedPk))
                .setAlgorithm(KEY_ALGORITHM)
                .setLevel(keyLevel)
                .setPublicKey(serializedPk)
                .build();

    }

    @Override
    public Signer createSigner(Key.Level keyLevel) {
        boolean requiresAuth =  !(keyLevel == Key.Level.LOW);
        return new AKSSigner(getKeyFromKeyStore(keyLevel), requiresAuth, context);
    }

    @Override
    public Verifier createVerifier(String keyId) {
        return new AKSVerifier(getKeyFromKeyStore(keyId));
    }

    private KeyStore.Entry getKeyFromKeyStore(Key.Level keyLevel) {
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            KeyStore.Entry entry = ks.getEntry(getAlias(keyLevel), null);
            if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
                throw new RuntimeException("Invalid private key");
            }
            return entry;
        } catch (UnrecoverableEntryException |
                KeyStoreException |
                IOException |
                NoSuchAlgorithmException |
                CertificateException
                exception) {
            throw new RuntimeException(exception);
        }
    }

    private KeyStore.Entry getKeyFromKeyStore(String keyId) {
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            Enumeration<String> aliases = ks.aliases();
            while(aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                byte[] publicKey = ks.getCertificate(alias).getPublicKey().getEncoded();
                if (keyIdFor(ByteEncoding.serialize(publicKey)).equals(keyId)) {
                    return ks.getEntry(alias, null);
                }
            }
        } catch (UnrecoverableEntryException |
                KeyStoreException |
                IOException |
                NoSuchAlgorithmException |
                CertificateException
                exception) {
            throw new RuntimeException(exception);
        }
        throw new KeyNotFoundException(keyId);
    }

    private String getAlias(Key.Level keyLevel) {
        return KEY_NAME + "-" + memberId + "-" + keyLevel.toString();
    }

    public static String keyIdFor(String serializedKey) {
        byte[] encoded = ByteEncoding.parse(serializedKey);
        byte[] hash = Hashing.sha256().newHasher()
                .putBytes(encoded)
                .hash()
                .asBytes();
        return ByteEncoding.serialize(hash).substring(0, 16);
    }
}
