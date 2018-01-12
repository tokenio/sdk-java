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
import java.security.KeyStore.Entry;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.Signature;
import java.security.SignatureException;

/**
 * Signs payloads using keys in the Android KeyStore.
 */
public class AKSSigner implements Signer{
    private final Entry entry;
    private final Key.Level keyLevel;
    private final UserAuthenticationStore userAuthenticationStore;
    private final int authenticationTimeSeconds;

    AKSSigner(
            Entry entry,
            Key.Level keyLevel,
            UserAuthenticationStore userAuthenticationStore,
            int authenticationTimeSeconds) {
        this.entry = entry;
        this.keyLevel = keyLevel;
        this.userAuthenticationStore = userAuthenticationStore;
        this.authenticationTimeSeconds = authenticationTimeSeconds;
    }

    /**
     * Gets the keyId by serializing and hashing the public key.
     *
     * @return keyId
     */
    @Override
    public String getKeyId() {
        byte[] publicKey = ((PrivateKeyEntry)entry).getCertificate().getPublicKey().getEncoded();
        return AKSCryptoEngine.keyIdFor(ByteEncoding.serialize(publicKey));
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
        Signature s = null;
        try {
            s = Signature.getInstance("SHA256withECDSA");
            s.initSign(((PrivateKeyEntry) entry).getPrivateKey());

            // If we are in an old device ad this is a privileged signer / operation
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M && keyLevel != Key.Level.LOW) {

                // If user authentication has expired
                if (System.currentTimeMillis() >= userAuthenticationStore.userAuthenticatedTime()
                                        + authenticationTimeSeconds * 1000) {
                    throw new TokenAuthenticationException(null);
                }
            }

            s.update(payload.getBytes("UTF-8"));
            byte[] signature = s.sign();

            // Only allow one privileged signature for each authentication
            userAuthenticationStore.expireUserAuthentication();

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
}
