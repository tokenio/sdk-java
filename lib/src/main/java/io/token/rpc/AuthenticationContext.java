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

package io.token.rpc;

import io.token.proto.common.security.SecurityProtos.Key;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authentication context. Stores the values of On-Behalf-Of and Key-Level in the
 * thread local storage to be used for request authentication and signing.
 */
public class AuthenticationContext {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationContext.class);
    private static final ThreadLocal<String> onBehalfOf = new ThreadLocal<>();
    private static final ThreadLocal<Key.Level> keyLevel = ThreadLocal.withInitial(
            new Supplier<Key.Level>() {
                @Override
                public Key.Level get() {
                    return Key.Level.LOW;
                }
            });

    /**
     * Retrieves the On-Behalf-Of value.
     *
     * @return the current On-Behalf-Of value
     */
    public static String getOnBehalfOf() {
        return onBehalfOf.get();
    }

    /**
     * Retrieves the Key-Level value.
     *
     * @return the current Key-Level value
     */
    public static Key.Level getKeyLevel() {
        return keyLevel.get();
    }

    /**
     * Sets the On-Behalf-Of value.
     *
     * @param tokenId the value of the On-Behalf-Of
     */
    public static void setOnBehalfOf(String tokenId) {
        logger.info("Authenticated On-Behalf-Of: {}", tokenId);
        onBehalfOf.set(tokenId);
    }

    /**
     * Sets the Key-Level value.
     *
     * @param level the value of the key level
     */
    public static void setKeyLevel(Key.Level level) {
        keyLevel.set(level);
    }

    /**
     * Retrieves and clears an On-Behalf-Of value.
     *
     * @return an On-Behalf-Of value or null
     */
    public static String clearOnBehalfOf() {
        String tokenId = onBehalfOf.get();
        onBehalfOf.remove();
        return tokenId;
    }

    /**
     * Retrieves and resets a Key-Level value.
     *
     * @return a Key-Level value
     */
    public static Key.Level resetKeyLevel() {
        Key.Level level = keyLevel.get();
        keyLevel.set(Key.Level.LOW);
        return level;
    }

    /**
     * Resets the authenticator.
     */
    public static void clear() {
        onBehalfOf.remove();
        keyLevel.set(Key.Level.LOW);
    }
}
