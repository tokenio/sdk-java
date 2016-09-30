package io.token.rpc;

import com.google.common.base.Strings;
import com.google.protobuf.Message;
import io.grpc.Metadata;
import io.grpc.Status;
import io.token.rpc.interceptor.SimpleInterceptor;
import io.token.security.Crypto;
import io.token.security.SecretKey;

import java.util.Optional;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

/**
 * gRPC interceptor that performs Token authentication by signing the request
 * with a member private key.
 */
final class ClientAuthenticator<ReqT, ResT> implements SimpleInterceptor<ReqT, ResT> {
    private final String memberId;
    private final String alias;
    private final SecretKey key;

    ClientAuthenticator(String memberId, String alias, SecretKey key) {
        this.memberId = memberId;
        this.alias = alias;
        this.key = key;
    }

    @Override
    public void onStart(ReqT reqT, Metadata metadata) {
        String signature = Crypto.sign(key, (Message) reqT);
        metadata.put(Metadata.Key.of("token-realm", ASCII_STRING_MARSHALLER), "Token");
        metadata.put(Metadata.Key.of("token-scheme", ASCII_STRING_MARSHALLER), "Token-Ed25519-SHA512");
        metadata.put(Metadata.Key.of("token-key-id", ASCII_STRING_MARSHALLER), key.getId());
        metadata.put(Metadata.Key.of("token-signature", ASCII_STRING_MARSHALLER), signature);

        if (!Strings.isNullOrEmpty(memberId)) {
            metadata.put(Metadata.Key.of("token-member-id", ASCII_STRING_MARSHALLER), memberId);
        } else {
            metadata.put(Metadata.Key.of("token-alias", ASCII_STRING_MARSHALLER), alias);
        }
    }

    @Override
    public void onComplete(Status status, ReqT req, Optional<ResT> res, Optional<Metadata> trailers) {}
}
