package io.token;

import io.grpc.ManagedChannel;
import io.token.proto.bankapi.AccountLinkingServiceGrpc;
import io.token.proto.bankapi.AccountLinkingServiceGrpc.AccountLinkingServiceBlockingStub;
import io.token.proto.bankapi.Banklink.AuthorizeLinkAccountsRequest;
import io.token.proto.bankapi.Banklink.AuthorizeLinkAccountsResponse;
import io.token.proto.bankapi.Fank;
import io.token.proto.bankapi.FankServiceGrpc;
import io.token.proto.bankapi.FankServiceGrpc.FankServiceBlockingStub;
import io.token.proto.common.money.MoneyProtos;
import io.token.rpc.client.RpcChannelFactory;

import java.util.List;

import static java.lang.String.format;


public final class BankClient {
    private final FankServiceBlockingStub fank;
    private final AccountLinkingServiceBlockingStub accountLinking;

    public BankClient(String hostName, int port) {
        ManagedChannel channel = RpcChannelFactory.forTarget(format("dns:///%s:%d/", hostName, port));
        this.fank = FankServiceGrpc.newBlockingStub(channel);
        this.accountLinking = AccountLinkingServiceGrpc.newBlockingStub(channel);
    }

    public Fank.Client addClient(String firstName, String lastName) {
        Fank.AddClientResponse response = fank.addClient(Fank.AddClientRequest.newBuilder()
                .setFirstName(firstName)
                .setFirstName(lastName)
                .build());
        return response.getClient();
    }

    public Fank.Account addAccount(Fank.Client client, String name, String number, double amount, String currency) {
        Fank.AddAccountResponse response = fank.addAccount(Fank.AddAccountRequest.newBuilder()
                .setClientId(client.getId())
                .setName(name)
                .setAccountNumber(number)
                .setBalance(MoneyProtos.Money.newBuilder()
                        .setValue(Double.toString(amount))
                        .setCurrency(currency))
                .build());
        return response.getAccount();
    }

    public List<String> startAccountsLinking(
            String clientId,
            List<String> accountNumbers) {
        AuthorizeLinkAccountsRequest request = AuthorizeLinkAccountsRequest.newBuilder()
                .setClientId(clientId)
                .addAllAccounts(accountNumbers)
                .build();
        AuthorizeLinkAccountsResponse response = accountLinking.authorizeLinkAccounts(request);
        return response.getAccountLinkPayloadsList();
    }
}
