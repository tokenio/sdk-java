package io.token.sample;

import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createMemberAndLinkAccounts;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.notification.NotificationProtos.Notification;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;

import java.util.Optional;

import org.junit.Test;

public class PollNotificationsSampleTest {

    @Test
    public void notifyPaymentRequestSampleTest() {
        try (TokenIO tokenIO = createClient()) {
            Member payer = PollNotificationsSample.createMember(tokenIO);
            Member payee = createMemberAndLinkAccounts(tokenIO);

            LinkMemberAndBankSample.linkBankAccounts(payer);

            NotifyStatus status = NotifyPaymentRequestSample.notifyPaymentRequest(
                    tokenIO,
                    payee,
                    payer.firstAlias());
            assertThat(status).isNotNull();
            Optional<Notification> notification = PollNotificationsSample.poll(payer);
            assertThat(notification.isPresent()).isTrue();
        }
    }
}
