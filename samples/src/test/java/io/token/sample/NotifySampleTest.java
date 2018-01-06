package io.token.sample;

import static io.token.sample.CreateAndEndorseAccessTokenSample.createAccessToken;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createMemberAndLinkAccounts;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.notification.NotificationProtos.RequestStepUp.RequestType;
import io.token.proto.common.token.TokenProtos.Token;

import org.junit.Test;

public class NotifySampleTest {
    @Test
    public void notifyPaymentRequestSampleTest() {
        try (TokenIO tokenIO = createClient()) {
            Alias payerAlias = randomAlias();
            Member payer = tokenIO.createMember(payerAlias);
            Member payee = createMemberAndLinkAccounts(tokenIO);

            LinkMemberAndBankSample.linkBankAccounts(payer);

            NotifyStatus status = NotifySample.notifyPaymentRequest(
                    tokenIO,
                    payee,
                    payerAlias);
            assertThat(status).isNotNull();
        }
    }

    @Test
    public void triggerTokenStepUpNotificationTest() {
        try (TokenIO tokenIO = createClient()) {
            Member grantor = tokenIO.createMember(randomAlias());
            Alias granteeAlias = randomAlias();
            Member grantee = tokenIO.createMember(granteeAlias);

            Token token = createAccessToken(grantor, granteeAlias);
            NotifyStatus status = grantor.triggerTokenStepUpNotification(token.getId());

            assertThat(status).isNotNull();
        }
    }

    @Test
    public void triggerRequestStepUpNotificationTest() {
        try (TokenIO tokenIO = createClient()) {
            Member member = tokenIO.createMember(randomAlias());

            NotifyStatus status = member.triggerRequestStepUpNotification(RequestType.GET_BALANCE);
            assertThat(status).isNotNull();
        }
    }

    @Test
    public void notifyExpiredAccessTokenTest() {
        try (TokenIO tokenIO = createClient()) {
            Member grantor = tokenIO.createMember(randomAlias());
            Alias granteeAlias = randomAlias();
            Member grantee = tokenIO.createMember(granteeAlias);

            Token token = createAccessToken(grantor, granteeAlias);

            NotifyStatus status = grantee.notifyExpiredAccessToken(token.getId());
            assertThat(status).isNotNull();
        }
    }
}
