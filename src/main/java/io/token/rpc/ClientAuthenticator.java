package io.token.rpc;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import io.grpc.Metadata;
import io.grpc.Status;
import io.token.proto.gateway.Auth.GRpcAuthPayload;
import io.token.rpc.interceptor.SimpleInterceptor;
import io.token.security.Signer;

import java.time.Instant;
import java.util.Optional;

/**
 * gRPC interceptor that performs Token authentication by signing the request
 * with a member private key.
 */
final class ClientAuthenticator<ReqT, ResT> implements SimpleInterceptor<ReqT, ResT> {
    private final String memberId;
    private final String username;
    private final Signer signer;

    ClientAuthenticator(String memberId, String username, Signer signer) {
        this.memberId = memberId;
        this.username = username;
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


        if (!Strings.isNullOrEmpty(memberId)) {
            metadata.put(Metadata.Key.of("token-member-id", ASCII_STRING_MARSHALLER), memberId);
        } else {
            metadata.put(Metadata.Key.of("token-username", ASCII_STRING_MARSHALLER), username);
        }

        String onBehalfOf = AuthenticationContext.clearOnBehalfOf();
        if (!Strings.isNullOrEmpty(onBehalfOf)) {
            metadata.put(
                    Metadata.Key.of("token-on-behalf-of", ASCII_STRING_MARSHALLER),
                    onBehalfOf);
        }
    }

    @Override
    public void onComplete(
            Status status,
            ReqT req,
            Optional<ResT> res,
            Optional<Metadata> trailers) {
    }
}
