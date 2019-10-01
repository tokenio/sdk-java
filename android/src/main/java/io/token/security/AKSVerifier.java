package io.token.security;

import com.google.protobuf.Message;
import io.token.proto.ProtoJson;
import io.token.util.codec.ByteEncoding;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;

/**
 * Verifies signatures from Android KeyStore;
 */
public class AKSVerifier implements Verifier {
    private final Certificate certificate;

    /**
     * Creates a KeyStore verifier.
     *
     * @param certificate certificate in the Android KeyStore
     */
    AKSVerifier(Certificate certificate) {
        this.certificate = certificate;
    }

    /**
     * Verifies the signature, first converting to JSON. Throws if verification fails.
     *
     * @param message protobuf message
     * @param signature signature
     */
    @Override
    public void verify(Message message, String signature) {
        verify(ProtoJson.toJson(message), signature);
    }

    /**
     * Verifies the signature on the string payload. Throws if verification fails.
     *
     * @param payload message
     * @param signature signature
     */
    @Override
    public void verify(String payload, String signature) {
        try {
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initVerify(certificate);
            s.update(payload.getBytes("UTF-8"));
            boolean verified = s.verify(ByteEncoding.parse(signature));
            if (!verified) {
                throw new InvalidSignatureException("Invalid signature");
            }
        } catch (InvalidKeyException | NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        } catch (SignatureException ex) {
            throw new InvalidSignatureException(ex);
        }
    }
}
