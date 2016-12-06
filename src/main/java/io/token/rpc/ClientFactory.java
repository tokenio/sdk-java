package io.token.rpc;

import io.grpc.ManagedChannel;
import io.token.proto.gateway.GatewayServiceGrpc;
import io.token.rpc.client.RpcChannelFactory;
import io.token.security.Signer;

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
     * @return newly created client
     */
    static Client authenticated(ManagedChannel channel, String memberId, String username, Signer signer) {
        GatewayServiceGrpc.GatewayServiceFutureStub stub = GatewayServiceGrpc.newFutureStub(
                RpcChannelFactory.intercept(
                        channel,
                        new ClientAuthenticatorFactory(memberId, username, signer)
                )
        );
        return new Client(memberId, signer, stub);
    }
}
