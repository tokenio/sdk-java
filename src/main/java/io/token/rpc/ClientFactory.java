package io.token.rpc;

import io.grpc.ManagedChannel;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.gateway.GatewayServiceGrpc;
import io.token.rpc.client.RpcChannelFactory;
import io.token.security.CryptoEngine;

/**
 * A factory class that is used to create {@link Client} and {@link UnauthenticatedClient}
 * instances.
 */
public interface ClientFactory {
    /**
     * Creates new unauthenticated client backed by the specified channel.
     *
     * @param channel RPC channel to use
     * @return newly created client
     */
    static UnauthenticatedClient unauthenticated(ManagedChannel channel) {
        return new UnauthenticatedClient(GatewayServiceGrpc.newFutureStub(channel));
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
    static Client authenticated(
            ManagedChannel channel,
            String memberId,
            CryptoEngine crypto) {
        GatewayServiceGrpc.GatewayServiceFutureStub stub = GatewayServiceGrpc.newFutureStub(
                RpcChannelFactory.intercept(
                        channel,
                        new ClientAuthenticatorFactory(memberId, crypto.createSigner(Key.Level.LOW))
                )
        );
        return new Client(memberId, crypto, stub);
    }
}
