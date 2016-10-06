package io.token;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.grpc.ManagedChannel;
import io.token.proto.bankapi.AccountLinkingServiceGrpc;
import io.token.proto.bankapi.AccountLinkingServiceGrpc.AccountLinkingServiceFutureStub;
import io.token.proto.bankapi.Banklink.LinkAccountsRequest;
import io.token.proto.bankapi.Banklink.LinkAccountsResponse;
import io.token.rpc.client.RpcChannelFactory;
import rx.Observable;

import java.util.List;
import java.util.Optional;

import static io.token.rpc.util.Converters.toObservable;
import static java.lang.String.format;


public final class BankClient {
    private final AccountLinkingServiceFutureStub client;

    public BankClient(String hostName, int port) {
        ManagedChannel channel = RpcChannelFactory.forTarget(format("dns:///%s:%d/", hostName, port));
        this.client = AccountLinkingServiceGrpc.newFutureStub(channel);
    }

    public String startAccountsLinking(
            String alias,
            Optional<String> secret,
            List<String> accountNumbers,
            Optional<Message> metadata) {
        return startAccountsLinkingAsync(alias, secret, accountNumbers, metadata)
                .toBlocking()
                .single();
    }

    public Observable<String> startAccountsLinkingAsync(
            String alias,
            Optional<String> secret,
            List<String> accountNumbers,
            Optional<Message> metadata) {
        LinkAccountsRequest.Builder builder = LinkAccountsRequest.newBuilder();
        metadata.ifPresent(data -> builder.setMetadata(Any.pack(data)));
        builder
                .setAlias(alias)
                .addAllAccounts(accountNumbers)
                .setSecret(secret.orElse(""))
                .build();
        return toObservable(client.linkAccounts(builder.build()))
                .map(LinkAccountsResponse::getAccountsLinkPayload);
    }
}
