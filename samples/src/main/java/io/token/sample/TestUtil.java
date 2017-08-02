package io.token.sample;

import io.token.util.Util;

/**
 * A set of helper methods used for testing.
 */
public abstract class TestUtil {
    private TestUtil() {}

    /**
     * Generates random user name to be used for testing.
     *
     * @return random user name
     */
    public static String newAlias() {
        return "alias-" + Util.generateNonce();
    }
}
