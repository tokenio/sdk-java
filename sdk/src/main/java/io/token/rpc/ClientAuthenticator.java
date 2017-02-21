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

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import io.grpc.Metadata;
import io.token.proto.gateway.Auth.GRpcAuthPayload;
import io.token.rpc.interceptor.SimpleInterceptor;
import io.token.security.Signer;

import java.time.Instant;

/**
 * gRPC interceptor that performs Token authentication by signing the request
 * with a member private key.
 */
final class ClientAuthenticator<ReqT, ResT> implements SimpleInterceptor<ReqT, ResT> {
    private final String memberId;
    private final Signer signer;

    ClientAuthenticator(String memberId, Signer signer) {
        this.memberId = memberId;
        this.signer = signer;
    }

    @Override
    public void onStart(ReqT reqT, Metadata metadata) {
        Instant now = Instant.now();
        GRpcAuthPayload payload = GRpcAuthPayload.newBuilder()
                .setRequest(ByteString.copyFrom(((Message) reqT).toByteArray()))
                .setCreatedAtMs(now.toEpochMilli())
                .build();
        String signature = signer.sign(payload);

        metadata.put(Metadata.Key.of("token-realm", ASCII_STRING_MARSHALLER), "Token");
        metadata.put(
                Metadata.Key.of("token-scheme", ASCII_STRING_MARSHALLER),
                "Token-Ed25519-SHA512");
        metadata.put(Metadata.Key.of("token-key-id", ASCII_STRING_MARSHALLER), signer.getKeyId());
        metadata.put(Metadata.Key.of("token-signature", ASCII_STRING_MARSHALLER), signature);
        metadata.put(
                Metadata.Key.of("token-created-at-ms", ASCII_STRING_MARSHALLER),
                Long.toString(now.toEpochMilli()));
        metadata.put(Metadata.Key.of("token-member-id", ASCII_STRING_MARSHALLER), memberId);

        String onBehalfOf = AuthenticationContext.clearOnBehalfOf();
        if (!Strings.isNullOrEmpty(onBehalfOf)) {
            metadata.put(
                    Metadata.Key.of("token-on-behalf-of", ASCII_STRING_MARSHALLER),
                    onBehalfOf);
        }
    }
}
