package io.token;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.token.proto.gateway.Gateway.CreateMemberRequest;
import io.token.proto.gateway.Gateway.CreateMemberResponse;
import io.token.proto.gateway.GatewayServiceGrpc;
import io.token.proto.gateway.GatewayServiceGrpc.GatewayServiceFutureStub;
import io.token.rpc.client.RpcChannelFactory;
import io.token.rx.Converters;
import io.token.security.KeyGenerator;
import io.token.security.KeyPair;
import rx.Observable;

import static io.token.Util.generateNonce;
import static java.lang.String.format;

public final class Token {
    public static final class Builder {
        private String hostName;
        private int port;

        public Builder hostName(String hostName) {
            this.hostName = hostName;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Token build() {
            ManagedChannel channel = RpcChannelFactory.forTarget(format("dns:///%s:%d/", hostName, port));
            GatewayServiceFutureStub gateway = GatewayServiceGrpc.newFutureStub(channel);
            return new Token(gateway);
        }
    }

    private final KeyGenerator keyGenerator;
    private final GatewayServiceFutureStub gateway;

    public static Builder builder() {
        return new Builder();
    }

    private Token(GatewayServiceFutureStub gateway) {
        this.keyGenerator = new KeyGenerator();
        this.gateway = gateway;
    }

    public Member createMember(String memberName) {
        return createMemberAsync(memberName).toBlocking().single();
    }

    public Observable<Member> createMemberAsync(String memberName) {
        KeyPair keyPair = keyGenerator.generateKeyPair();
        ListenableFuture<CreateMemberResponse> response = gateway.createMember(CreateMemberRequest.newBuilder()
                .setNonce(generateNonce())
                .setPublicKey(ByteString.copyFrom(keyPair.getPublicKey()))
                .setMemberName(memberName)
                .build());
        return Converters
                .toObservable(response)
                .map(r -> new Member(memberName, r.getMemberId(), keyPair, r.getKeyId()));
    }
}
