package io.token.security;

import com.google.protobuf.Message;
import io.token.proto.ProtoJson;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenSignature.Action;

import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

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
     * Signs the supplied token with the specified key.
     *
     * @param key key to use for signing
     * @param token token to sign
     * @param action action being signed on
     * @return token signature
     */
    public static String sign(SecretKey key, Token token, Action action) {
        String payload = Stream
                .of(ProtoJson.toJson(token.getPayload()), action.name().toLowerCase())
                .collect(joining("."));
        return new Signer(dsa, key.getPrivateKey()).sign(payload);
    }

    /**
     * Signs the specified payload using given key
     *
     * @param key the key to be used for signing
     * @param payload the payload to be signed
     * @return a payload signature
     */
    public static String sign(SecretKey key, String payload) {
        return new Signer(dsa, key.getPrivateKey()).sign(payload);
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
