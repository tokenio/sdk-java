package io.token.sample;

import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;

import org.junit.Test;

public class NotifySampleTest {
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
