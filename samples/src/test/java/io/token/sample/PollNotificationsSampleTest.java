package io.token.sample;

import static io.token.TokenIO.TokenCluster.DEVELOPMENT;
import static io.token.sample.TestUtil.newAlias;
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
        try (TokenIO tokenIO = TokenIO.create(
                DEVELOPMENT,
                "4qY7lqQw8NOl9gng0ZHgT4xdiDqxqoGVutuZwrUYQsI")) {
            Member payer = PollNotificationsSample.createMember(tokenIO);
            Member payee = tokenIO.createMember(newAlias());

            LinkMemberAndBankSample.linkBankAccounts(payer);
            LinkMemberAndBankSample.linkBankAccounts(payee);

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
