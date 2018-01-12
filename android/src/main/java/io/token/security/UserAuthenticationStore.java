package io.token.security;

/**
 * Stores the last time the user authenticated. This is used for old phones, where we cannot
 * use the KeyStore to check for user authentication.
 */
public class UserAuthenticationStore {
    private static final int AUTHENTICATION_DURATION_SECONDS_DEFAULT = 5;
    private final int authenticationDurationSeconds;
    private long userAuthenticatedTime = 0;

    /**
     * Creates a store for authentication time of user
     *
     * @param authenticationDurationSeconds how many seconds the authentication lasts for
     */
    public UserAuthenticationStore(int authenticationDurationSeconds) {
        this.authenticationDurationSeconds = authenticationDurationSeconds;
    }

    public UserAuthenticationStore() {
        this.authenticationDurationSeconds = AUTHENTICATION_DURATION_SECONDS_DEFAULT;
    }

    public void authenticateUser() {
        userAuthenticatedTime = System.currentTimeMillis();
    }

    public void expireUserAuthentication() {
        userAuthenticatedTime = 0;
    }

    public boolean isAuthenticated() {
        return (System.currentTimeMillis() <
                userAuthenticatedTime + authenticationDurationSeconds * 1000);
    }

    public int authenticationDurationSeconds() {
        return authenticationDurationSeconds;
    }
}
