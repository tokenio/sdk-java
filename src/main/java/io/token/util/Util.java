package io.token.util;

import static com.google.common.base.Throwables.propagate;
import static io.token.proto.common.security.SecurityProtos.Key.Algorithm.ED25519;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

import io.token.proto.common.security.SecurityProtos.Key.Algorithm;
import io.token.security.crypto.CryptoType;

import java.security.NoSuchAlgorithmException;

public interface Util {
    /**
     * Generates a random string.
     *
     * @return generated random string
     */
    static String generateNonce() {
        return randomAlphabetic(20);
    }

    /**
     * Converts String algorithm into proto representation.
     *
     * @param cryptoType the type of the algorithm
     * @return a proto algorithm
     */
    static Algorithm toProtoAlgorithm(CryptoType cryptoType) {
        switch (cryptoType) {
            case EDDSA:
                return ED25519;
            default:
                throw propagate(new NoSuchAlgorithmException(cryptoType.toString()));
        }
    }
}
