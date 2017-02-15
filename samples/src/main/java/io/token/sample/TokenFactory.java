package io.token.sample;

import io.token.util.Util;

/**
 * A helper factory to initialize the SDK.
 */
public interface TokenFactory {
    /**
     * Generates random user name to be used for testing.
     *
     * @return random user name
     */
    static String newUserName() {
        return "username-" + Util.generateNonce();
    }
}
