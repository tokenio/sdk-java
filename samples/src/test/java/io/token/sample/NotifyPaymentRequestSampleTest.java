package io.token.sample;

import static io.token.TokenIO.TokenCluster.DEVELOPMENT;
import static io.token.TokenIO.TokenCluster.SANDBOX;
import static io.token.sample.TestUtil.newAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;

import org.junit.Test;

public class NotifyPaymentRequestSampleTest {

    @Test
    public void notifyPaymentRequestTest() {
        try (TokenIO tokenIO = TokenIO.create(DEVELOPMENT)) {
            Member payer = tokenIO.createMember(newAlias());
            Member payee = tokenIO.createMember(newAlias());

            LinkMemberAndBankSample.linkBankAccounts(payer);
            LinkMemberAndBankSample.linkBankAccounts(payee);

            NotifyStatus status = NotifyPaymentRequestSample.notifyPaymentRequest(
                    tokenIO,
                    payee,
                    payer.firstAlias());
            assertThat(status).isNotNull();
        }
    }
}
