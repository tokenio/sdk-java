package io.token.util;

import static com.google.common.base.Throwables.propagate;
import static io.token.proto.common.security.SecurityProtos.Key.Algorithm.ED25519;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

import io.token.proto.common.member.MemberProtos;
import io.token.proto.common.member.MemberProtos.MemberOperation;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.security.SecurityProtos.Key.Algorithm;
import io.token.security.crypto.CryptoType;

import java.security.NoSuchAlgorithmException;

/**
 * Utility methods.
 */
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

    /**
     * Converts Key to AddKey operation.
     *
     * @param key key to add
     * @return member operation
     */
    static MemberOperation toAddKeyOperation(Key key) {
        return MemberOperation.newBuilder()
                .setAddKey(MemberProtos.MemberAddKeyOperation.newBuilder().setKey(key)).build();
    }

    /**
     * Converts username to AddUsername operation.
     *
     * @param username username to add
     * @return member operation
     */
    static MemberOperation toAddUsernameOperation(String username) {
        return MemberOperation.newBuilder()
                .setAddUsername(MemberProtos.MemberUsernameOperation.newBuilder()
                        .setUsername(username))
                .build();

    }
}
