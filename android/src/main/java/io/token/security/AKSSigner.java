package io.token.security;

import android.content.Context;
import com.google.protobuf.Message;
import io.token.proto.ProtoJson;
import io.token.util.codec.ByteEncoding;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.Signature;

public class AKSSigner implements Signer{
    private final Entry entry;
    private boolean requiresAuth;
    private Context context;

    AKSSigner(Entry entry, boolean requiresAuth, Context context) {
        this.entry = entry;
        this.requiresAuth = requiresAuth;
        this.context = context;
    }
    @Override
    public String getKeyId() {
        byte[] publicKey = ((PrivateKeyEntry)entry).getCertificate().getPublicKey().getEncoded();
        return AKSCryptoEngine.keyIdFor(ByteEncoding.serialize(publicKey));
    }

    @Override
    public String sign(Message message) {
        return sign(ProtoJson.toJson(message));
    }

    @Override
    public String sign(String payload) {
        try {
            if (requiresAuth) {
                // TODO: Use context to prompt user for signature
            }
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initSign(((PrivateKeyEntry) entry).getPrivateKey());
            s.update(payload.getBytes("UTF-8"));
            byte[] signature = s.sign();
            return ByteEncoding.serialize(signature);
        } catch (GeneralSecurityException | UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }
}
