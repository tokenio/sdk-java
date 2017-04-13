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

import io.token.proto.common.security.SecurityProtos;

/**
 * Provides key storage primitives.
 */
public interface KeyStore {
    /**
     * Puts a specified key into the storage.
     *
     * @param memberId member ID
     * @param key key to put into the storage
     * @throws KeyIOException if an error is encountered while storing the key
     */
    void put(String memberId, SecretKey key);

    /**
     * Gets a key by its level.
     *
     * @param memberId member ID
     * @param keyLevel {@link SecurityProtos.Key.Level} of the key to get
     * @return secret key
     * @throws KeyIOException if an error is encountered while fetching the key
     */
    SecretKey getByLevel(String memberId, SecurityProtos.Key.Level keyLevel);

    /**
     * Gets a key by its ID.
     *
     * @param memberId member ID
     * @param keyId key ID to get
     * @return secret key
     * @throws KeyIOException if an error is encountered while fetching the key
     */
    SecretKey getById(String memberId, String keyId);
}
