package io.token.sample;

import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createMemberAndLinkAccounts;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.user.Member;
import io.token.user.TokenClient;

import org.junit.Test;

public class NotifySampleTest {
    @Test
    public void notifyPaymentRequestSampleTest() {
        try (TokenClient tokenClient = createClient()) {
            Alias payerAlias = randomAlias();
            Member payer = tokenClient.createMemberBlocking(payerAlias);
            Member payee = createMemberAndLinkAccounts(tokenClient);

            LinkMemberAndBankSample.linkBankAccounts(payer);

            NotifyStatus status = NotifySample.notifyPaymentRequest(
                    tokenClient,
                    payee,
                    payerAlias);
            assertThat(status).isNotNull();
        }
    }
}
