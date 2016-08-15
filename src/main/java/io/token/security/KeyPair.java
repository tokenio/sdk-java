package io.token.security;

import static org.apache.commons.codec.binary.Hex.encodeHexString;

public class KeyPair {
    private final byte[] privateKey;
    private final byte[] publicKey;

    public KeyPair(byte[] privateKey, byte[] publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public byte[] getPrivateKey() {
        return privateKey;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    @Override
    public String toString() {
        return encodeHexString(publicKey);
    }
}
