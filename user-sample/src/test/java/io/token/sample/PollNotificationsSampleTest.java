package io.token.sample;

import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createMemberAndLinkAccounts;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.notification.NotificationProtos.Notification;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferDestination;
import io.token.user.Account;
import io.token.user.Member;
import io.token.user.PrepareTokenResult;
import io.token.user.TokenClient;
import io.token.user.TransferTokenBuilder;

import java.util.Optional;

import org.junit.Test;

public class PollNotificationsSampleTest {

    @Test
    public void notifyPaymentRequestSampleTest() {
        try (TokenClient tokenClient = createClient()) {
            Member payer = createMemberAndLinkAccounts(tokenClient);

            Member payee = PollNotificationsSample.createMember(tokenClient);
            Alias payeeAlias = payee.firstAliasBlocking();
            Account account = LinkMemberAndBankSample.linkBankAccounts(payer);
            LinkMemberAndBankSample.linkBankAccounts(payee);

            TransferDestination tokenDestination = TransferDestination.newBuilder()
                    .setToken(TransferDestination.Token.newBuilder()
                                    .setMemberId(payee.memberId()))
                    .build();

            TransferTokenBuilder builder = payer.createTransferTokenBuilder(100.00, "EUR")
                    .setAccountId(account.id())
                    .setToAlias(payeeAlias)
                    .addDestination(tokenDestination);

            PrepareTokenResult result = payer.prepareTransferTokenBlocking(builder);
            Token token = payer.createTokenBlocking(result.getTokenPayload(), Key.Level.STANDARD);
            Transfer transfer = payee.redeemTokenBlocking(token);

            Optional<Notification> notification = PollNotificationsSample.poll(payee);

            assertThat(notification.isPresent()).isTrue();
        }
    }
}
