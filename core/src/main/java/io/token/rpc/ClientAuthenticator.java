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

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static io.token.rpc.ContextKeys.CUSTOMER_DEVICE_ID_KEY;
import static io.token.rpc.ContextKeys.CUSTOMER_GEO_LOCATION_KEY;
import static io.token.rpc.ContextKeys.CUSTOMER_IP_ADDRESS_KEY;

import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import io.grpc.Metadata;
import io.token.exceptions.KeyNotFoundException;
import io.token.proto.common.security.SecurityProtos.CustomerTrackingMetadata;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.security.SecurityProtos.Key.Level;
import io.token.proto.gateway.Auth.GrpcAuthPayload;
import io.token.rpc.interceptor.SimpleInterceptor;
import io.token.security.CryptoEngine;
import io.token.security.Signer;

/**
 * gRPC interceptor that performs Token authentication by signing the request
 * with a member private key.
 */
final class ClientAuthenticator<ReqT, ResT> extends SimpleInterceptor<ReqT, ResT> {
    private final String memberId;
    private final CryptoEngine crypto;
    private final AuthenticationContext authenticationContext;

    ClientAuthenticator(
            String memberId,
            CryptoEngine crypto,
            AuthenticationContext authenticationContext) {
        this.memberId = memberId;
        this.crypto = crypto;
        this.authenticationContext = authenticationContext;
    }

    @Override
    public void onStart(ReqT reqT, Metadata metadata) {
        long now = System.currentTimeMillis();
        GrpcAuthPayload payload = GrpcAuthPayload.newBuilder()
                .setRequest(ByteString.copyFrom(((Message) reqT).toByteArray()))
                .setCreatedAtMs(now)
                .build();
        Key.Level keyLevel = authenticationContext.getKeyLevel();
        Signer signer = createSigner(keyLevel);
        String signature = signer.sign(payload);

        metadata.put(Metadata.Key.of("token-realm", ASCII_STRING_MARSHALLER), "Token");
        metadata.put(
                Metadata.Key.of("token-scheme", ASCII_STRING_MARSHALLER),
                "Token-Ed25519-SHA512");
        metadata.put(Metadata.Key.of("token-key-id", ASCII_STRING_MARSHALLER), signer.getKeyId());
        metadata.put(Metadata.Key.of("token-signature", ASCII_STRING_MARSHALLER), signature);
        metadata.put(
                Metadata.Key.of("token-created-at-ms", ASCII_STRING_MARSHALLER),
                Long.toString(now));
        metadata.put(Metadata.Key.of("token-member-id", ASCII_STRING_MARSHALLER), memberId);

        CustomerTrackingMetadata customer = authenticationContext.getCustomerTrackingMetadata();
        if (!customer.getIpAddress().isEmpty()) {
            metadata.put(CUSTOMER_IP_ADDRESS_KEY.getMetadataKey(), customer.getIpAddress());
        }
        if (!customer.getGeoLocation().isEmpty()) {
            metadata.put(CUSTOMER_GEO_LOCATION_KEY.getMetadataKey(), customer.getGeoLocation());
        }
        if (!customer.getDeviceId().isEmpty()) {
            metadata.put(CUSTOMER_DEVICE_ID_KEY.getMetadataKey(), customer.getDeviceId());
        }

        String onBehalfOf = authenticationContext.getOnBehalfOf();
        if (!Strings.isNullOrEmpty(onBehalfOf)) {
            metadata.put(
                    Metadata.Key.of("token-on-behalf-of", ASCII_STRING_MARSHALLER),
                    onBehalfOf);
            metadata.put(
                    Metadata.Key.of("customer-initiated", ASCII_STRING_MARSHALLER),
                    Boolean.toString(authenticationContext.getCustomerInitiated()));
        }
    }

    @Override
    public void onHalfClose(ReqT req, Metadata headers) {
        // Ignore
    }

    private Signer createSigner(Level minKeyLevel) {
        Level keyLevel = minKeyLevel;
        while (keyLevel.getNumber() > 0) {
            try {
                return crypto.createSigner(keyLevel);
            } catch (KeyNotFoundException e) {
                // try a key for the next level
                keyLevel = Level.forNumber(keyLevel.getNumber() - 1);
            }
        }
        throw new KeyNotFoundException("Key not found for level " + minKeyLevel + " or higher.");
    }
}
