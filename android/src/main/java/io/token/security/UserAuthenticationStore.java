package io.token.security;

/**
 * Stores the last time the user authenticated. This is used for old phones, where we cannot
 * use the KeyStore to check for user authentication.
 */
public class UserAuthenticationStore {
    long userAuthenticatedTime = 0;
    public UserAuthenticationStore() {}

    public void authenticateUser() {
        userAuthenticatedTime = System.currentTimeMillis();
    }

    public void expireUserAuthentication() {

    }

    public long userAuthenticatedTime() {
        return userAuthenticatedTime;
    }
}
