package io.token.sample;

import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createMemberAndLinkAccounts;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.notification.NotificationProtos.Notification;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.user.Account;
import io.token.user.Destinations;
import io.token.user.Member;
import io.token.user.TokenClient;

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

            TokenProtos.Token token = payer.createTransferToken(100.00, "EUR")
                    .setAccountId(account.id())
                    .setToAlias(payeeAlias)
                    .addDestination(Destinations.token(payee.memberId()))
                    .execute();
            payer.endorseTokenBlocking(token, Key.Level.STANDARD);
            Transfer transfer = payee.redeemTokenBlocking(token);

            Optional<Notification> notification = PollNotificationsSample.poll(payee);

            assertThat(notification.isPresent()).isTrue();
        }
    }
}
