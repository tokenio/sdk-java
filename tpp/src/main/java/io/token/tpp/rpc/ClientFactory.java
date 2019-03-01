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

package io.token.tpp.rpc;

import io.grpc.ManagedChannel;
import io.token.rpc.GatewayProvider;
import io.token.security.CryptoEngine;

/**
 * A factory class that is used to create {@link Client} and {@link UnauthenticatedClient}
 * instances.
 */
public abstract class ClientFactory {
    private ClientFactory() {
    }

    /**
     * Creates new unauthenticated client backed by the specified channel.
     *
     * @param channel RPC channel to use
     * @return newly created client
     */
    public static UnauthenticatedClient unauthenticated(ManagedChannel channel) {
        GatewayProvider serviceBuilder = new GatewayProvider(channel);
        return new UnauthenticatedClient(serviceBuilder);
    }

    /**
     * Creates authenticated client backed by the specified channel. The supplied
     * signer is used to authenticate the caller for every call.
     *
     * @param channel RPC channel to use
     * @param memberId member id
     * @param crypto crypto engine to use for signing requests, tokens, etc
     * @return newly created client
     */
    public static Client authenticated(
            final ManagedChannel channel,
            final String memberId,
            final CryptoEngine crypto) {
        GatewayProvider serviceBuilder = new GatewayProvider(channel, memberId, crypto);
        return new Client(memberId, crypto, serviceBuilder);
    }
}
