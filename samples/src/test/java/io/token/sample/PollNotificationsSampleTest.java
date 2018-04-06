package io.token.sample;

import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createMemberAndLinkAccounts;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Account;
import io.token.Destinations;
import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.notification.NotificationProtos.Notification;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos;
import io.token.proto.common.transfer.TransferProtos.Transfer;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

public class PollNotificationsSampleTest {

    @Test
    public void notifyPaymentRequestSampleTest() {
        try (TokenIO tokenIO = createClient()) {
            Member payer = createMemberAndLinkAccounts(tokenIO);

            Member payee = PollNotificationsSample.createMember(tokenIO);

            Alias payeeAlias = payee.firstAlias();
            Account account = LinkMemberAndBankSample.linkBankAccounts(payer);
            LinkMemberAndBankSample.linkBankAccounts(payee);

            TokenProtos.Token token = payer.createTransferToken(100.00, "EUR")
                    .setAccountId(account.id())
                    .setToAlias(payeeAlias)
                    .addDestination(Destinations.token(payee.memberId()))
                    .setRedeemerMemberId(payer.memberId())
                    .execute();
            payer.endorseToken(token, Key.Level.STANDARD);
            Transfer transfer = payer.redeemToken(token);

            Optional<Notification> notification = PollNotificationsSample.poll(payee);

            assertThat(notification.isPresent()).isTrue();
        }
    }
}
