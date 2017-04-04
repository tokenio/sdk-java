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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import io.token.proto.common.security.SecurityProtos;

import java.util.Collection;

/**
 * In memory implementation of the {@link KeyStore}. Used for testing.
 */
public final class InMemoryKeyStore implements KeyStore {
    private final Multimap<String, SecretKey> keys = ArrayListMultimap.create();

    @Override
    public void put(String memberId, SecretKey key) {
        keys.put(memberId, key);
    }

    @Override
    public SecretKey getByLevel(String memberId, SecurityProtos.Key.Level keyLevel) {
        Collection<SecretKey> memberKeys = keys.get(memberId);
        for (SecretKey key : memberKeys) {
            if (key.getLevel().equals(keyLevel)) {
                return key;
            }
        }
        throw new IllegalArgumentException("Key not found for level: " + keyLevel);
    }

    @Override
    public SecretKey getById(String memberId, String keyId) {
        Collection<SecretKey> memberKeys = keys.get(memberId);
        for (SecretKey key : memberKeys) {
            if (key.getId().equals(keyId)) {
                return key;
            }
        }
        throw new IllegalArgumentException("Key not found for id: " + keyId);
    }
}
