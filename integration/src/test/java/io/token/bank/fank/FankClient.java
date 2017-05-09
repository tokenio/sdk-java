/**
 * Copyright (C) 2017 Token, Inc.
 * <p>
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package io.token.bank.fank;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import io.token.proto.bankapi.Fank;
import io.token.proto.bankapi.Fank.AddAccountResponse;
import io.token.proto.bankapi.Fank.AddClientResponse;
import io.token.proto.bankapi.Fank.AuthorizeLinkAccountsRequest;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.proto.common.money.MoneyProtos;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

final class FankClient {
    private final FankClientApi fankApi;

    public FankClient(String hostName, int port, boolean useSsl) {
        String protocol = useSsl ? "https" : "http";
        String urlFormat = "%s://%s:%d";
        fankApi = new Retrofit.Builder()
                .baseUrl(String.format(urlFormat, protocol, hostName, port))
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
                .create(FankClientApi.class);
    }

    public Fank.Client addClient(String firstName, String lastName) {
        AddClientResponse response = wrap(
                fankApi.addClient(protoToJson(
                        Fank.AddClientRequest.newBuilder()
                                .setFirstName(firstName)
                                .setFirstName(lastName))),
                AddClientResponse.newBuilder());
        return response.getClient();
    }

    public Fank.Account addAccount(
            Fank.Client client,
            String name,
            String number,
            double amount,
            String currency) {
        AddAccountResponse response = wrap(
                fankApi.addAccount(
                        protoToJson(Fank.AddAccountRequest.newBuilder()
                                .setClientId(client.getId())
                                .setName(name)
                                .setAccountNumber(number)
                                .setBalance(MoneyProtos.Money.newBuilder()
                                        .setValue(Double.toString(amount))
                                        .setCurrency(currency))),
                        client.getId()),
                AddAccountResponse.newBuilder());
        return response.getAccount();
    }

    public BankAuthorization startAccountsLinking(
            String username,
            String clientId,
            List<String> accountNumbers) {
        return wrap(
                fankApi.authorizeLinkAccounts(
                        protoToJson(AuthorizeLinkAccountsRequest.newBuilder()
                                .setUsername(username)
                                .setClientId(clientId)
                                .addAllAccounts(accountNumbers)),
                        clientId),
                BankAuthorization.newBuilder());
    }

    private String protoToJson(Message.Builder proto) {
        try {
            return JsonFormat.printer().print(proto);
        } catch (InvalidProtocolBufferException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Message> T wrap(Call<String> call, T.Builder builder) {
        try {
            JsonFormat
                    .parser()
                    .ignoringUnknownFields()
                    .merge(call.execute().body(), builder);
            return (T) builder.build();
        } catch (IOException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }
}