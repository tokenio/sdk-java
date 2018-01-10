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

import android.content.Context;

/**
 * Creates {@link io.token.security.AKSCryptoEngine} instances bound to a given member id.
 * Uses a provided context to draw authentication UI.
 */
public class AKSCryptoEngineFactory implements io.token.security.CryptoEngineFactory {
    private final Context context;
    private final UserAuthenticationStore store;

    /**
     * Creates a new instance of the factory.
     *
     * @param context context
     */
    public AKSCryptoEngineFactory(Context context, UserAuthenticationStore store) {
        this.context = context;
        this.store = store;
    }

    /**
     * Creates a new {@link io.token.security.AKSCryptoEngine} for the given member.
     *
     * @param memberId member id
     * @return crypto engine instance
     */
    @Override
    public io.token.security.CryptoEngine create(String memberId) {
        return new AKSCryptoEngine(memberId, context, store);
    }
}
