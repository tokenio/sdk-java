/**
 * Copyright (c) 2021 Token, Inc.
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

import io.grpc.MethodDescriptor;
import io.token.rpc.interceptor.InterceptorFactory;
import io.token.rpc.interceptor.SimpleInterceptor;
import io.token.security.CryptoEngine;

/**
 * Responsible for creation of {@link ClientAuthenticator} instances which
 * are created per RPC method call.
 */
public final class ClientAuthenticatorFactory implements InterceptorFactory {
    private final String memberId;
    private final CryptoEngine crypto;
    private final AuthenticationContext authenticationContext;

    /**
     * Creates an instance.
     *
     * @param memberId Token member ID
     * @param crypto crypto engine
     * @param authenticationContext authentication context
     */
    public ClientAuthenticatorFactory(
            String memberId,
            CryptoEngine crypto,
            AuthenticationContext authenticationContext) {
        this.memberId = memberId;
        this.crypto = crypto;
        this.authenticationContext = authenticationContext;
    }

    @Override
    public <ReqT, ResT> SimpleInterceptor<ReqT, ResT> create(MethodDescriptor<ReqT, ResT> ignore) {
        return new ClientAuthenticator<>(memberId, crypto, authenticationContext);
    }
}
