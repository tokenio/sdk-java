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

import com.google.auto.value.AutoValue;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.util.Clock;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.annotation.Nullable;

/**
 * Encapsulates secret key data.
 */
@AutoValue
public abstract class SecretKey {
    public static SecretKey create(
            String id,
            Key.Level level,
            KeyPair keyPair) {
        return new AutoValue_SecretKey(id, level, keyPair.getPublic(), keyPair.getPrivate(), null);
    }

    /**
     * Creates an instance of SecretKey.
     *
     * @param id key id
     * @param level key level
     * @param keyPair key pair
     * @param expiresAtMs expiration date of the key in milliseconds
     * @return SecretKey instance
     */
    public static SecretKey create(
            String id,
            Key.Level level,
            KeyPair keyPair,
            @Nullable Long expiresAtMs) {
        return new AutoValue_SecretKey(
                id,
                level,
                keyPair.getPublic(),
                keyPair.getPrivate(),
                expiresAtMs);
    }

    public abstract String getId();

    public abstract Key.Level getLevel();

    public abstract PublicKey getPublicKey();

    public abstract PrivateKey getPrivateKey();

    @Nullable public abstract Long getExpiresAtMs();

    /**
     * Checks whether a key has expired against the provided clock.
     *
     * @param clock clock
     * @return true if key has expired
     */
    public boolean isExpired(Clock clock) {
        return getExpiresAtMs() != null && getExpiresAtMs() < clock.getTime();
    }
}
