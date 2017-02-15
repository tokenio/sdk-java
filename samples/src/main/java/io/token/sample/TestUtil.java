package io.token.sample;

import io.token.util.Util;

/**
 * A set of helper methods used for testing.
 */
public interface TestUtil {
    /**
     * Generates random user name to be used for testing.
     *
     * @return random user name
     */
    static String newUserName() {
        return "username-" + Util.generateNonce();
    }
}
