package io.token.sample;

import io.grpc.ManagedChannel;
import io.token.Member;
import io.token.Token.TokenCluster;
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
     * <p>
     * The bank linking is currently only supported by the Token PSD2 IOS mobile app.
     * This sample is implemented for a very high level illustration of the bank linking concept
     * and serves as an integral building block connecting other samples. The desktop version of
     * the bank linking process is in the development. Until it's ready, please use Token PSD2 IOS
     * mobile app to link Token members and banks.
     *
     * @param tokenCluster Token cluster to connect to (e.g.: TokenCluster.PRODUCTION)
     * @return a new Member instance
     */
    public static Member linkBank(TokenCluster tokenCluster) {
        // Create a new token member using the CreateMemberSample.
        Member member = CreateMemberSample.createMember(tokenCluster);

        // User opens a bank web site and completes the Token linking process. The following is a
        // high level description of how that happens in the Token PSD2 IOS mobile app:
        // 1. App displays a list of banks supported by Token
        // 2. User selects a bank, the app pops up a web view and navigates to the bank linking page
        // 3. User enters bank credentials and selects accounts to link
        // 4. The bank linking flow completes and the app extracts encrypted account linking
        //    payload from the internal service redirected to by the linking flow.

        // For the purpose of this sample, we simulate the entire linking flow described above
        // by generating it with a fake bank below.
        List<SealedMessage> encryptedLinkingPayloads =
                getPayloadsList(member, tokenCluster).getPayloadsList();

        // Finish account linking flow initiated by the user.
        member.linkAccounts("iron" /* bank code */, encryptedLinkingPayloads);

        return member;
    }

    private static Banklink.AccountLinkingPayloads getPayloadsList(
            Member member,
            TokenCluster tokenCluster) {
        // ********************** Fake bank simulation fragment start ************************** //
        // The below code fragment is a sample demonstration of what happens on a bank side
        // when a user tries to link her account(s) with Token. This is not a part of the Token
        // SDK and is only required to make this sample runnable. Feel free to ignore it.

        // Simulate a real bank service by using a fake bank ("fank"), adding a fake bank client
        // "John Doe" with a checking account and some balance.
        ManagedChannel channel = RpcChannelFactory
                .builder(tokenCluster.url().replace("api", "fank"), 443, true /* useSsl */)
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
