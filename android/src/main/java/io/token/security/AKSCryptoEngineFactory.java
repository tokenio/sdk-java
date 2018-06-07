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
    private final UserAuthenticationStore userAuthenticationStore;
    private final boolean useSecureHardwareKeystoreOnly;

    /**
     * Creates a new instance of the factory. If useSecureHardwareKeystoreOnly is true and insecure
     * keystore is detected, a SecureHardwareKeystoreRequiredException error will be thrown.
     *
     * @param context context
     * @param userAuthenticationStore stores the last time the user authenticated
     * @param useSecureHardwareKeystoreOnly true if use secure hardware keystore only
     */
    public AKSCryptoEngineFactory(Context context,
                                  UserAuthenticationStore userAuthenticationStore,
                                  boolean useSecureHardwareKeystoreOnly) {
        this.context = context;
        this.userAuthenticationStore = userAuthenticationStore;
        this.useSecureHardwareKeystoreOnly = useSecureHardwareKeystoreOnly;
    }

    /**
     * Creates a new instance of the factory. Supports secure hardware keystore only.
     *
     * @param context context
     * @param userAuthenticationStore stores the last time the user authenticated
     */
    public AKSCryptoEngineFactory(Context context,
                                  UserAuthenticationStore userAuthenticationStore) {
        this(context, userAuthenticationStore, true);
    }

    /**
     * Creates a new {@link io.token.security.AKSCryptoEngine} for the given member.
     *
     * @param memberId member id
     * @return crypto engine instance
     */
    @Override
    public io.token.security.CryptoEngine create(String memberId) {
        return new AKSCryptoEngine(
                memberId,
                context,
                userAuthenticationStore,
                useSecureHardwareKeystoreOnly);
    }
}
