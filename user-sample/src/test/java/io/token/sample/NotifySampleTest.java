package io.token.sample;

import static io.token.sample.CreateAndEndorseAccessTokenSample.createAccessToken;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createMemberAndLinkAccounts;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.token.TokenProtos.Token;
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

    @Test
    public void triggerBalanceStepUpNotificationTest() {
        try (TokenClient tokenClient = createClient()) {
            Member member = tokenClient.createMemberBlocking(randomAlias());

            NotifyStatus status = member.triggerBalanceStepUpNotificationBlocking(
                    ImmutableList.of(
                            "123",
                            "456"));
            assertThat(status).isNotNull();
        }
    }
}
