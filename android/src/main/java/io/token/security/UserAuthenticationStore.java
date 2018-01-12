package io.token.security;

import java.security.Signature;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores the last time the user authenticated. This is used for old phones, where we cannot
 * use the KeyStore to check for user authentication.
 */
public class UserAuthenticationStore {
    private long userAuthenticatedTime = 0;
    private final Map<String, Signature> signatureObjectCache;

    public UserAuthenticationStore() {
        signatureObjectCache = new HashMap<>();
    }

    public void authenticateUser() {
        userAuthenticatedTime = System.currentTimeMillis();
    }

    public void expireUserAuthentication() {
        userAuthenticatedTime = 0;
    }

    public long userAuthenticatedTime() {
        return userAuthenticatedTime;
    }

    public Signature getSignature(String payload) {
        return signatureObjectCache.get(payload);
    }

    public void putSignature(String payload, Signature signature) {
        signatureObjectCache.put(payload, signature);
    }
}
