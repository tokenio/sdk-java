package io.token.util;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

public interface Util {
    static String generateNonce() {
        return randomAlphabetic(20);
    }
}
