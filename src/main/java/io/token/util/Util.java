package io.token.util;

import static com.google.common.base.Throwables.propagate;
import static io.token.proto.common.security.SecurityProtos.Key.Algorithm.ED25519;
import static io.token.security.crypto.EdDsaCrypto.EDDSA;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

import io.token.proto.common.security.SecurityProtos.Key.Algorithm;

import java.security.NoSuchAlgorithmException;

public interface Util {
    /**
     * Generates a random string.
     */
    static String generateNonce() {
        return randomAlphabetic(20);
    }

    /**
     * Converts String algorithm into proto representation.
     *
     * @param algorithm the name of the algorithm
     * @return a proto algorithm
     */
    static Algorithm toProtoAlgorithm(String algorithm) {
        switch (algorithm) {
            case EDDSA :
                return ED25519;
            default:
                throw propagate(new NoSuchAlgorithmException(algorithm));
        }
    }
}
