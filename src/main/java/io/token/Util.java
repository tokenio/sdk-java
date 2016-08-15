package io.token;

import org.apache.commons.lang.RandomStringUtils;

interface Util {
    static String generateNonce() {
        return RandomStringUtils.randomAlphabetic(20);
    }
}
