package io.token.security;

import android.os.Build;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.UserNotAuthenticatedException;
import com.google.protobuf.Message;
import io.token.proto.ProtoJson;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.security.exceptions.TokenAuthenticationException;
import io.token.security.exceptions.TokenInvalidKeyException;
import io.token.util.codec.ByteEncoding;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;

/**
 * Signs payloads using keys in the Android KeyStore.
 */
public class AKSSigner implements Signer {
    private static final String ALGORITHM = "SHA256withECDSA";
    private final PrivateKey privateKey;
    private final Certificate certificate;
    private final Key.Level keyLevel;
    private final UserAuthenticationStore userAuthenticationStore;

    /**
     * Creates a KeyStore signer.
     *
     * @param privateKey privateKey in the Android KeyStore
     * @param certificate certificate in the Android KeyStore
     * @param keyLevel level of the key in the entry
     * @param userAuthenticationStore store for user authentication
     */
    AKSSigner(
        PrivateKey privateKey,
        Certificate certificate,
        Key.Level keyLevel,
        UserAuthenticationStore userAuthenticationStore) {
        this.privateKey = privateKey;
        this.certificate = certificate;
        this.keyLevel = keyLevel;
        this.userAuthenticationStore = userAuthenticationStore;
    }

    /**
     * Gets the keyId by serializing and hashing the public key.
     *
     * @return keyId
     */
    @Override
    public String getKeyId() {
        return AKSCryptoEngine.keyIdFor(certificate);
    }

    /**
     * Signs the protobuf message, first converting to JSON.
     *
     * @param message Protobuf message
     * @return Signature
     */
    @Override
    public String sign(Message message) {
        return sign(ProtoJson.toJson(message));
    }

    /**
     * Signs the string, throwing an exception if the user has not recently authenticated.
     *
     * @param payload string payload
     * @return Signature
     */
    @Override
    public String sign(String payload) {
        Signature s;
        try {
            s = Signature.getInstance(ALGORITHM);
            s.initSign(privateKey);

            // If this is a privileged signer / operation
            if (keyLevel != Key.Level.LOW && !userAuthenticationStore.isAuthenticated()) {
                throw new TokenAuthenticationException(null);
            }

            s.update(payload.getBytes("UTF-8"));
            byte[] signature = s.sign();

            return ByteEncoding.serialize(signature);
        } catch (GeneralSecurityException | UnsupportedEncodingException ex) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ex instanceof UserNotAuthenticatedException || ex instanceof SignatureException) {
                    // Throws on new devices that haven't authenticated users. Checked by KeyStore. This
                    // only happens on new devices (Android M or later) which is why we do an additional
                    // check above, for the older devices. Before throwing, the signature is saved
                    // for later
                    throw new TokenAuthenticationException(null);
                } else if (ex instanceof KeyPermanentlyInvalidatedException) {
                    // Throws when the user has changed their device passcode or biometrics
                    throw new TokenInvalidKeyException(ex);
                }
            }
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String getAlgorithm() {
        return ALGORITHM;
    }
}
