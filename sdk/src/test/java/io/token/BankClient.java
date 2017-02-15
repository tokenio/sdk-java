/**
 * Copyright (C) 2017 Token, Inc.
 * <p>
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package io.token;

import io.grpc.ManagedChannel;
import io.token.proto.bankapi.Fank;
import io.token.proto.bankapi.Fank.AuthorizeLinkAccountsRequest;
import io.token.proto.bankapi.FankServiceGrpc;
import io.token.proto.bankapi.FankServiceGrpc.FankServiceBlockingStub;
import io.token.proto.banklink.Banklink;
import io.token.proto.common.money.MoneyProtos;
import io.token.proto.common.security.SecurityProtos.SealedMessage;
import io.token.rpc.client.RpcChannelFactory;

import java.io.Closeable;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;


public final class BankClient implements Closeable {
    private static final Duration SHUTDOWN_DURATION = Duration.ofSeconds(5);

    private final ManagedChannel channel;
    private final FankServiceBlockingStub fank;

    public BankClient(String hostName, int port, boolean useSsl) {
        this.channel = RpcChannelFactory.builder(hostName, port, useSsl).build();
        this.fank = FankServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public void close() {
        channel.shutdown();
        try {
            channel.awaitTermination(SHUTDOWN_DURATION.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
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
        Banklink.AccountLinkingPayloads response = fank.authorizeLinkAccounts(request);
        return response.getPayloadsList();
    }
}
