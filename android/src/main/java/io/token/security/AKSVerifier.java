package io.token.security;

import com.google.protobuf.Message;
import io.token.proto.ProtoJson;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;

public class AKSVerifier implements Verifier {
    private final Entry entry;

    AKSVerifier(Entry entry) {
        this.entry = entry;
    }

    @Override
    public void verify(Message message, String signature) {
        verify(ProtoJson.toJson(message), signature);
    }

    @Override
    public void verify(String payload, String signature) {
        try {
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initVerify(((PrivateKeyEntry) entry).getCertificate());
            s.update(payload.getBytes("UTF-8"));
            s.verify(signature.getBytes("UTF-8"));
        } catch (InvalidKeyException | NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        } catch (SignatureException ex) {
            throw new InvalidSignatureException(ex);
        }
    }
}
