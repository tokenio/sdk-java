package io.token.sample;

import io.grpc.ManagedChannel;
import io.token.Member;
import io.token.proto.bankapi.Fank;
import io.token.proto.bankapi.FankServiceGrpc;
import io.token.proto.banklink.Banklink;
import io.token.proto.common.money.MoneyProtos;
import io.token.proto.common.security.SecurityProtos.SealedMessage;
import io.token.rpc.client.RpcChannelFactory;

import java.util.List;

/**
 * Links a Token member and a bank.
 */
public final class LinkMemberAndBankSample {
    /**
     * Links a Token member created by {@link CreateMemberSample} and a bank.
     *
     * @param tokenApiUrl token API url (e.g.: "api-grpc.token.io")
     * @param bankServiceUrl url of a bank service that implements Token bank API
     *         (e.g.: "fank-grpc.token.io")
     * @return a new Member instance
     */
    public static Member linkBank(String tokenApiUrl, String bankServiceUrl) {
        // Create a new token member using the CreateMemberSample.
        Member member = CreateMemberSample.createMember(tokenApiUrl);

        // User starts an account linking process from a bank web site...

        // The bank linking flow generates an encrypted account linking payload that is sent to
        // a registered device via a push notification.
        List<SealedMessage> encryptedLinkingPayloads =
                getPayloadsList(bankServiceUrl, member).getPayloadsList();

        // Finish account linking flow initiated by the user.
        member.linkAccounts("iron" /* bank code */, encryptedLinkingPayloads);

        return member;
    }

    private static Banklink.AccountLinkingPayloads getPayloadsList(
            String bankServiceUrl,
            Member member) {
        // ********************** Fake bank simulation fragment start ************************** //
        // The below code fragment is a sample demonstration of what happens on a bank side
        // when a user tries to link her account(s) with Token. This is not a part of the Token
        // SDK and is only required to make this sample runnable. Feel free to ignore it.

        // Simulate a real bank service by using a fake bank ("fank"), adding a fake bank client
        // "John Doe" with a checking account and some balance.
        ManagedChannel channel = RpcChannelFactory
                .builder(bankServiceUrl, 443, true /* useSsl */)
                .build();

        FankServiceGrpc.FankServiceBlockingStub fakeBank = FankServiceGrpc.newBlockingStub(channel);

        // Add a fake bank client.
        Fank.AddClientResponse fakeClient = fakeBank.addClient(
                Fank.AddClientRequest.newBuilder()
                        .setFirstName("John")
                        .setFirstName("Doe")
                        .build());

        // Add a fake account.
        Fank.AddAccountResponse fakeAccount = fakeBank.addAccount(
                Fank.AddAccountRequest.newBuilder()
                        .setClientId(fakeClient.getClient().getId())
                        .setName("Checking")
                        .setAccountNumber("123456")
                        .setBalance(MoneyProtos.Money.newBuilder()
                                .setValue(Double.toString(1000.0))
                                .setCurrency("EUR"))
                        .build());

        Banklink.AccountLinkingPayloads accountLinkingPayloads = fakeBank.authorizeLinkAccounts(
                Fank.AuthorizeLinkAccountsRequest.newBuilder()
                        .setUsername(member.firstUsername())
                        .setClientId(fakeClient.getClient().getId())
                        .addAccounts(fakeAccount.getAccount().getAccountNumber())
                        .build());

        return accountLinkingPayloads;
        // ********************** Fake bank simulation fragment end ************************** //
    }
}
