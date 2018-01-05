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

import android.app.KeyguardManager;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.security.KeyPairGeneratorSpec;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.security.crypto.CryptoType;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import javax.inject.Inject;

/**
 * Token implementation of the {@link io.token.security.CryptoEngine}. The keys are persisted
 * in the provided storage
 */
public final class AKSCryptoEngine implements io.token.security.CryptoEngine {
    private static final CryptoType CRYPTO_TYPE = CryptoType.EDDSA;
    private static final Key.Algorithm KEY_ALGORITHM = Key.Algorithm.ED25519;

    public static final String KEY_NAME = "tokenapp_key";
    private final String memberId;
    private final Context context;
    @Inject KeyguardManager mKeyguardManager;
    @Inject FingerprintManager mFingerprintManager;
    private final KeyPairGenerator mKeyPairGenerator;

    /**
     * Creates an instance.
     *
     * @param memberId member ID
     * @param context context
     */
    public AKSCryptoEngine(String memberId,
                           Context context) {
        this.memberId = memberId;
        this.context = context;
        System.out.println("USING AKS ....");

        try {
            KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
                    .setAlias(KEY_NAME)
                    .setKeyType("EC")
                    .setKeySize(256)
                    .build();
            mKeyPairGenerator = KeyPairGenerator.getInstance(
                    "EC",
                    "AndroidKeyStore");
            mKeyPairGenerator.initialize(spec);
        } catch (NoSuchAlgorithmException | NoSuchProviderException |
                InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Key generateKey(Key.Level keyLevel) {
        System.out.println("Generating key...");
        KeyPair keyPair = mKeyPairGenerator.generateKeyPair();
        System.out.println(keyPair);
        System.out.println(keyPair.getPublic());
        System.out.println(keyPair.getPrivate().getAlgorithm());
        System.out.println(keyPair.getPrivate().getFormat());
        for (byte b : keyPair.getPublic().getEncoded()) {
            System.out.print(b + " : ");
        }
        System.out.println(keyPair.getPrivate());
        return Key.newBuilder()
//                .setId(key.getId())
//                .setAlgorithm(KEY_ALGORITHM)
//                .setLevel(keyLevel)
//                .setPublicKey(crypto.serialize(key.getPublicKey()))
                .build();
    }

    @Override
    public Signer createSigner(Key.Level keyLevel) {
//        io.token.security.SecretKey key = keyStore.getByLevel(memberId, keyLevel);
//        return crypto.signer(key.getId(), key.getPrivateKey());
        return null;
    }

    @Override
    public Verifier createVerifier(String keyId) {
        return null;
//        io.token.security.SecretKey key = keyStore.getById(memberId, keyId);
//        return crypto.verifier(key.getPublicKey());
    }
}
