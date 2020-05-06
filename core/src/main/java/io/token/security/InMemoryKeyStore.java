/**
 * Copyright (c) 2019 Token, Inc.
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

import static io.token.exceptions.KeyNotFoundException.keyExpired;
import static io.token.exceptions.KeyNotFoundException.keyNotFoundForId;
import static io.token.exceptions.KeyNotFoundException.keyNotFoundForLevel;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import io.token.proto.common.security.SecurityProtos;
import io.token.util.Clock;
import io.token.util.SystemTimeClock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * In memory implementation of the {@link KeyStore}. Used for testing.
 */
public final class InMemoryKeyStore implements KeyStore {
    private final Table<String, String, SecretKey> keys = HashBasedTable.create();
    private final Clock clock;

    @VisibleForTesting
    public InMemoryKeyStore(Clock clock) {
        this.clock = clock;
    }

    /**
     * Creates a new key store.
     */
    public InMemoryKeyStore() {
        this(new SystemTimeClock());
    }

    @Override
    public void put(String memberId, SecretKey key) {
        if (key.isExpired(clock)) {
            throw keyExpired(key.getId());
        }
        keys.put(memberId, key.getId(), key);
    }

    @Override
    public SecretKey getByLevel(String memberId, SecurityProtos.Key.Level keyLevel) {
        Collection<SecretKey> memberKeys = keys.row(memberId).values();
        for (SecretKey key : memberKeys) {
            if (key.getLevel().equals(keyLevel) && !key.isExpired(clock)) {
                return key;
            }
        }
        throw keyNotFoundForLevel(keyLevel);
    }

    @Override
    public SecretKey getById(String memberId, String keyId) {
        SecretKey key = keys.get(memberId, keyId);

        if (key == null) {
            throw keyNotFoundForId(keyId);
        }
        if (key.isExpired(clock)) {
            throw keyExpired(keyId);
        }

        return key;
    }

    @Override
    public List<SecretKey> listKeys(String memberId) {
        List<SecretKey> secretKeys = new ArrayList<>();
        for (SecretKey key : keys.row(memberId).values()) {
            if (!key.isExpired(clock)) {
                secretKeys.add(key);
            }
        }
        return secretKeys;
    }

    /**
     * Deletes keys for a specific member.
     *
     * @param memberId Id of member
     */
    @Override
    public void deleteKeys(String memberId) {
        Set<String> memberKeys = new HashSet<>(keys.row(memberId).keySet());
        for (String keyId : memberKeys) {
            keys.remove(memberId, keyId);
        }
    }
}
