package io.token.rpc;

import io.grpc.MethodDescriptor;
import io.token.rpc.interceptor.InterceptorFactory;
import io.token.rpc.interceptor.SimpleInterceptor;
import io.token.security.SecretKey;

/**
 * Responsible for creation of {@link ClientAuthenticator} instances which
 * are created per RCP method call.
 */
final class ClientAuthenticatorFactory implements InterceptorFactory {
    private final String memberId;
    private final SecretKey key;

    public ClientAuthenticatorFactory(String memberId, SecretKey key) {
        this.memberId = memberId;
        this.key = key;
    }

    @Override
    public <ReqT, ResT> SimpleInterceptor<ReqT, ResT> create(MethodDescriptor<ReqT, ResT> ignore) {
        return new ClientAuthenticator<>(memberId, key);
    }
}
