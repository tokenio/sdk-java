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

package io.token.security;

import io.token.proto.common.security.SecurityProtos.Key;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Encapsulates secret key data.
 */
final class SecretKey {
    private final String id;
    private final Key.Level level;
    private final KeyPair keyPair;

    /**
     * Creates an instance.
     *
     * @param id key ID
     * @param level key level
     * @param keyPair secret key pair
     */
    public SecretKey(String id, Key.Level level, KeyPair keyPair) {
        this.id = id;
        this.level = level;
        this.keyPair = keyPair;
    }

    /**
     * Gets key ID.
     *
     * @return key ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets key level.
     *
     * @return key level
     */
    public Key.Level getLevel() {
        return level;
    }

    /**
     * Gets public key.
     *
     * @return public key
     */
    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    /**
     * Gets private key.
     *
     * @return private key
     */
    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }
}
