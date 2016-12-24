package io.token;

import static java.lang.String.format;

import io.grpc.ManagedChannel;
import io.token.proto.bankapi.AccountLinkingServiceGrpc;
import io.token.proto.bankapi.AccountLinkingServiceGrpc.AccountLinkingServiceBlockingStub;
import io.token.proto.bankapi.Banklink.AuthorizeLinkAccountsRequest;
import io.token.proto.bankapi.Banklink.AuthorizeLinkAccountsResponse;
import io.token.proto.bankapi.Fank;
import io.token.proto.bankapi.FankServiceGrpc;
import io.token.proto.bankapi.FankServiceGrpc.FankServiceBlockingStub;
import io.token.proto.common.money.MoneyProtos;
import io.token.proto.common.security.SecurityProtos.SealedMessage;
import io.token.rpc.client.RpcChannelFactory;

import java.util.List;


public final class BankClient {
    private final FankServiceBlockingStub fank;
    private final AccountLinkingServiceBlockingStub accountLinking;

    public BankClient(String hostName, int port, boolean useSsl) {
        ManagedChannel channel = RpcChannelFactory.builder(
                hostName,
                port,
                useSsl).build();
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

    public Fank.Account addAccount(
            Fank.Client client,
            String name,
            String number,
            double amount,
            String currency) {
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

    public List<SealedMessage> startAccountsLinking(
            String username,
            String clientId,
            List<String> accountNumbers) {
        AuthorizeLinkAccountsRequest request = AuthorizeLinkAccountsRequest.newBuilder()
                .setUsername(username)
                .setClientId(clientId)
                .addAllAccounts(accountNumbers)
                .build();
        AuthorizeLinkAccountsResponse response = accountLinking.authorizeLinkAccounts(request);
        return response.getAccountLinkPayloadsList();
    }
}
