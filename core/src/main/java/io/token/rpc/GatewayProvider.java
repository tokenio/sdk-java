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

package io.token.rpc;

import io.grpc.ManagedChannel;
import io.token.proto.gateway.GatewayServiceGrpc;
import io.token.proto.gateway.GatewayServiceGrpc.GatewayServiceFutureStub;
import io.token.rpc.client.Interceptor;
import io.token.rpc.client.RpcChannelFactory;
import io.token.security.CryptoEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility for constructing a {@link GatewayServiceFutureStub} with optional interceptors
 * for client authentication and feature codes.
 */
public class GatewayProvider {
    private final String memberId;
    private final CryptoEngine cryptoEngine;
    private final GatewayServiceFutureStub stub;

    /**
     * Constructs an instance used for unauthenticated gateway calls.
     *
     * @param channel managed channel
     */
    public GatewayProvider(ManagedChannel channel) {
        this(channel, null, null);
    }

    /**
     * Constructs an instance used for authenticated gateway calls.
     *
     * @param channel managed channel
     * @param memberId member ID for authentication
     * @param cryptoEngine crypto engine for authentication
     */
    public GatewayProvider(
            ManagedChannel channel,
            String memberId,
            CryptoEngine cryptoEngine) {
        this.memberId = memberId;
        this.cryptoEngine = cryptoEngine;
        this.stub = GatewayServiceGrpc.newFutureStub(
                RpcChannelFactory.intercept(
                        channel,
                        new ErrorHandlerFactory()));
    }

    /**
     * Constructs a {@link GatewayServiceBuilder} for a specific request.
     *
     * @return {@link GatewayServiceBuilder}
     */
    GatewayServiceBuilder serviceBuilder() {
        return new GatewayServiceBuilder();
    }

    class GatewayServiceBuilder {
        private List<Interceptor> interceptors;

        private GatewayServiceBuilder() {
            this.interceptors = new ArrayList<>();
        }

        /**
         * Adds an interceptor for client authentication.
         *
         * @param context authentication context
         * @return builder
         */
        GatewayServiceBuilder withAuthentication(AuthenticationContext context) {
            if (memberId == null || cryptoEngine == null) {
                throw new IllegalArgumentException("Require member ID and crypto engine.");
            }
            interceptors.add(new Interceptor(new ClientAuthenticatorFactory(
                    memberId,
                    cryptoEngine,
                    context)));
            return this;
        }

        /**
         * Adds an interceptor for feature codes.
         *
         * @param featureCodes map of feature codes
         * @return builder
         */
        GatewayServiceBuilder withFeatureCodes(List<String> featureCodes) {
            if (!featureCodes.isEmpty()) {
                interceptors.add(new Interceptor(new FeatureCodeInterceptorFactory(featureCodes)));
            }
            return this;
        }

        /**
         * Constructs the {@link GatewayServiceFutureStub} with the specified interceptors.
         *
         * @return {@link GatewayServiceFutureStub}
         */
        GatewayServiceFutureStub build() {
            return stub
                    .withInterceptors(interceptors.toArray(new Interceptor[interceptors.size()]));
        }
    }
}
