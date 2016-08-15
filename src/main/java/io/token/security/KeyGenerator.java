package io.token.security;

public final class KeyGenerator {
    private final DSA dsa = new EdDSA();

    public KeyPair generateKeyPair() {
        byte[] privateKey = dsa.createPrivateKey();
        byte[] publicKey = dsa.publicKeyFor(privateKey);
        return new KeyPair(privateKey, publicKey);
    }
}
