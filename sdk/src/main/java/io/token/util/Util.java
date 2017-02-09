/**
 * Copyright (c) 2017 Token, Inc.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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
