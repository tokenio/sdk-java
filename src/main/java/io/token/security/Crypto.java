package io.token.security;

import com.google.protobuf.Message;

/**
 * Set of helper methods.
 */
public final class Crypto {
    private static final DSA dsa = new EdDSA();

    /**
     * Signs the supplied protobuf message with the specified key.
     *
     * @param key key to use for signing
     * @param message protobuf message to sign
     * @return message signature
     */
    public static String sign(SecretKey key, Message message) {
        return new Signer(dsa, key.getPrivateKey()).sign(message);
    }

    /**
     * Generates a new key pair.
     *
     * @return newly created key pair
     */
    public static SecretKey generateSecretKey() {
        byte[] privateKey = dsa.createPrivateKey();
        byte[] publicKey = dsa.publicKeyFor(privateKey);
        return new SecretKey(privateKey, publicKey);
    }

    private Crypto() {}
}
