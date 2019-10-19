/**
 * Copyright (c) 2019 Token, Inc.
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
import android.security.KeyChain;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;
import com.google.common.hash.Hashing;
import io.token.exceptions.SecureHardwareKeystoreRequiredException;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.security.SecurityProtos.Key.Algorithm;
import io.token.proto.common.security.SecurityProtos.Key.Level;
import io.token.util.codec.ByteEncoding;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.security.auth.x500.X500Principal;

/**
 * Android KeyStore implementation of the {@link CryptoEngine}. The keys are
 * persisted in the android KeyStore, stored in hardware if the device supports it.
 */
public final class AKSCryptoEngine implements CryptoEngine {
    private static final String KEY_NAME = "tokenapp_key";
    private static final Algorithm KEY_ALGORITHM = Algorithm.ECDSA_SHA256;

    private final String memberId;
    private final Context context;
    private final KeyStore keyStore;
    private final UserAuthenticationStore userAuthenticationStore;
    private final boolean useSecureHardwareKeystoreOnly;

    /**
     * Creates an instance. If useSecureHardwareKeystoreOnly is true and insecure keystore is
     * detected, a SecureHardwareKeystoreRequiredException error will be thrown.
     *
     * @param memberId                      member ID
     * @param context                       context, to draw UI
     * @param userAuthenticationStore       stores the last time the user authenticated
     * @param useSecureHardwareKeystoreOnly true if use secure hardware keystore only
     */
    public AKSCryptoEngine(
            String memberId,
            Context context,
            UserAuthenticationStore userAuthenticationStore,
            boolean useSecureHardwareKeystoreOnly) {
        this.memberId = memberId;
        this.context = context;
        this.userAuthenticationStore = userAuthenticationStore;
        this.useSecureHardwareKeystoreOnly = useSecureHardwareKeystoreOnly;
        try {
            this.keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
        } catch (GeneralSecurityException | IOException ex) {
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
    public Key generateKey(Level keyLevel) {
        String serializedPk = generatePublicKey(keyLevel, null);
        return Key.newBuilder()
                .setId(keyIdFor(serializedPk))
                .setAlgorithm(KEY_ALGORITHM)
                .setLevel(keyLevel)
                .setPublicKey(serializedPk)
                .build();
    }

    /**
     * Generates an ECDSA private key and returns the public key. The private key is stored in the
     * Android KeyStore, and will never leave the secure hardware, on phones that support it. Two
     * different classes are used to create the key, based on phone version.
     *
     * @param keyLevel    key privilege level
     * @param expiresAtMs key expiration date in milliseconds
     * @return public key
     */
    @Override
    public Key generateKey(Level keyLevel, long expiresAtMs) {
        String serializedPk = generatePublicKey(keyLevel, expiresAtMs);
        return Key.newBuilder()
                .setId(keyIdFor(serializedPk))
                .setAlgorithm(KEY_ALGORITHM)
                .setLevel(keyLevel)
                .setPublicKey(serializedPk)
                .setExpiresAtMs(expiresAtMs)
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
    public Signer createSigner(Level keyLevel) {
        String alias = lookUpUnexpiredAliasInKeyStore(keyLevel);
        if (alias == null) {
            throw new IllegalArgumentException("No key found at level: " + keyLevel);
        }

        try {
            java.security.Key key = keyStore.getKey(alias, null);
            if (!(key instanceof PrivateKey)) {
                throw new RuntimeException("Invalid private key");
            }

            return new AKSSigner(
                (PrivateKey) key,
                keyStore.getCertificate(alias),
                keyLevel,
                userAuthenticationStore);
        } catch (GeneralSecurityException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Enumerates keystore and returns all public keys.
     *
     * @return list of public keys
     */
    @Override
    public List<Key> getPublicKeys() {
        try {
            List<Key> keys = new LinkedList<>();
            keyStore.load(null);
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                Certificate certificate = keyStore.getCertificate(alias);
                if (certificate == null) {
                    // There isn't any public key associated with this alias.
                    continue;
                }
                byte[] publicKey = certificate.getPublicKey().getEncoded();
                String serializedPk = ByteEncoding.serialize(publicKey);
                Level keyLevel = getKeyLevel(alias);
                String memberId = getMemberId(alias);
                if (keyLevel == null || !this.memberId.equals(memberId)) {
                    // skip if the key in the keystore is not a AKSCryptoEngine-managed key
                    // or if the member id does not match
                    continue;
                }
                keys.add(Key.newBuilder()
                        .setId(keyIdFor(serializedPk))
                        .setAlgorithm(KEY_ALGORITHM)
                        .setLevel(keyLevel)
                        .setPublicKey(serializedPk)
                        .build());
            }
            return keys;
        } catch (GeneralSecurityException | IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Creates a verifier object that verifies signatures made by the key in the KeyStore.
     *
     * @param keyId key id
     * @return Verifier
     */
    @Override
    public Verifier createVerifier(String keyId) {
        return new AKSVerifier(lookUpUnexpiredCertificateInKeyStore(keyId));
    }

    /**
     * Looks up an unexpired alias in the keystore, by the keyLevel.
     *
     * @param keyLevel keyLevel
     * @return alias
     */
    private String lookUpUnexpiredAliasInKeyStore(Level keyLevel) {
        try {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (alias.startsWith(getAliasPrefix(keyLevel)) && !isExpired(alias)) {
                    return alias;
                }
            }
            return null;
        } catch (GeneralSecurityException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Looks up an unexpired certificate in the keystore, by the keyId.
     *
     * @param keyId keyId
     * @return certificate
     */
    private Certificate lookUpUnexpiredCertificateInKeyStore(String keyId) {
        try {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                Certificate certificate = keyStore.getCertificate(alias);
                if (keyIdFor(certificate).equals(keyId)) {
                    if (isExpired(alias)) {
                        throw new IllegalArgumentException(
                                "Key with id: " + keyId + " has expired");
                    }
                    return certificate;
                }
            }
        } catch (GeneralSecurityException exception) {
            throw new RuntimeException(exception);
        }
        throw new KeyNotFoundException(keyId);
    }

    /**
     * Generates an alias for a key of a certain level.
     *
     * @param keyLevel    keyLevel
     * @param expiresAtMs expiration date in milliseconds
     * @return alias
     */
    private String createAlias(Level keyLevel, @Nullable Long expiresAtMs) {
        String alias = String.format("%s-%s-%s-%s",
                KEY_NAME, memberId, keyLevel.toString(),
                UUID.randomUUID().toString().replace("-", ""));
        // TODO(Luke) when API level < 23 does not need to be supported anymore,
        // can set keyValidityEnd and fetch it from KeyInfo instead of encoding it here.
        if (expiresAtMs != null) {
            alias = alias + "-" + expiresAtMs;
        }
        return alias;
    }

    private String getAliasPrefix(Level keyLevel) {
        return String.format("%s-%s-%s", KEY_NAME, memberId, keyLevel.toString());
    }

    /**
     * Gets the key level for a particular key store alias.
     *
     * @param alias key store alias
     * @return keyLevel
     */
    private static Level getKeyLevel(String alias) {
        String[] aliasParts = alias.split("-");
        if (aliasParts.length < 3) {
            return null;
        }
        return Level.valueOf(aliasParts[2]);
    }

    /**
     * Gets the expiration date of the key associated with the alias.
     *
     * @param alias key store alias
     * @return expiration date in milliseconds
     */
    private static Long getExpiration(String alias) {
        String[] aliasParts = alias.split("-");
        if (aliasParts.length < 5) {
            return null;
        }
        return Long.parseLong(aliasParts[4]);
    }

    /**
     * Gets the member id for a particular key store alias.
     *
     * @param alias key store alias
     * @return member id
     */
    private static String getMemberId(String alias) {
        String[] aliasParts = alias.split("-");
        if (aliasParts.length < 3) {
            return null;
        }
        return aliasParts[1];
    }

    private String generatePublicKey(Level keyLevel, @Nullable Long expiresAtMs) {
        KeyPairGenerator kpg;
        KeyPair kp;

        String alias = createAlias(keyLevel, expiresAtMs);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // API 23 and higher, uses new API. This allows for the use of EC keys.
                // Trusted hardware is used if it's available.
                KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                        alias,
                        KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                        .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                        .setDigests(KeyProperties.DIGEST_SHA256);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // On Android N and above, make sure user authentication is required for the key
                    // Android M has a bug where authentication loops, so require N instead. For any
                    // key that is not low privilege, user authentication is required.
                    // Authentication is checked by the KeyStore, and trusted hardware, if it's
                    // available.
                    //
                    // We can also invalidate the key if the user changes their biometrics
                    builder = builder
                            .setInvalidatedByBiometricEnrollment(true)
                            .setUserAuthenticationRequired(keyLevel != Level.LOW)
                            .setUserAuthenticationValidityDurationSeconds(
                                    userAuthenticationStore.authenticationDurationSeconds());
                    if (expiresAtMs != null) {
                        Calendar time = Calendar.getInstance();
                        time.setTimeInMillis(expiresAtMs);
                        builder = builder.setKeyValidityEnd(time.getTime());
                    }
                }

                kpg = KeyPairGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");

                kpg.initialize(builder.build());
            } else {
                // Uses old method of generating keys.
                Calendar start = Calendar.getInstance();
                Calendar end = Calendar.getInstance();
                if (expiresAtMs == null) {
                    end.add(Calendar.YEAR, 256);
                } else {
                    end.setTimeInMillis(expiresAtMs);
                }
                KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
                        .setAlias(alias)
                        .setKeyType(KeyProperties.KEY_ALGORITHM_EC)
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

            kp = kpg.generateKeyPair();

            if (useSecureHardwareKeystoreOnly) {
                boolean isSecureHardwareKeystoreSupported;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    KeyFactory keyFactory = KeyFactory.getInstance(
                            kp.getPrivate().getAlgorithm(),
                            "AndroidKeyStore");
                    KeyInfo keyInfo = keyFactory.getKeySpec(kp.getPrivate(), KeyInfo.class);
                    isSecureHardwareKeystoreSupported = keyInfo.isInsideSecureHardware();
                } else {
                    isSecureHardwareKeystoreSupported =
                            KeyChain.isBoundKeyAlgorithm(KeyProperties.KEY_ALGORITHM_RSA);
                }

                if (!isSecureHardwareKeystoreSupported) {
                    throw new SecureHardwareKeystoreRequiredException();
                }
            }
        } catch (NoSuchAlgorithmException | NoSuchProviderException |
                InvalidAlgorithmParameterException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }

        return ByteEncoding.serialize(kp.getPublic().getEncoded());
    }

    private boolean isExpired(String alias) {
        Long expiration = getExpiration(alias);
        return expiration != null && expiration < Calendar.getInstance().getTimeInMillis();
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

    /**
     * Calculates the keyId by hashing the key bytes.
     *
     * @param certificate certificate in the Android KeyStore
     * @return keyId
     */
    public static String keyIdFor(Certificate certificate) {
        byte[] publicKeyEncoded = certificate.getPublicKey().getEncoded();
        return keyIdFor(ByteEncoding.serialize(publicKeyEncoded));
    }

    @Override
    public void deleteKeys() {
        try {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                String mId = getMemberId(alias);
                if (mId == null) {
                    // It is not an alias that token can recognize.
                    continue;
                }
                if (mId.equals(memberId)) {
                    keyStore.deleteEntry(alias);
                }
            }
        } catch (GeneralSecurityException exception) {
            throw new RuntimeException(exception);
        }
    }
}
