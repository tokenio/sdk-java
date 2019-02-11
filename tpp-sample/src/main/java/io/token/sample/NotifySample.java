package io.token.sample;

import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.tpp.Member;

import java.util.List;

public class NotifySample {
    /**
     * Triggers a notification to step up the signature level when requesting balance information.
     *
     * @param member member
     * @param accountIds list of account id
     * @return notification status
     */
    public static NotifyStatus triggerBalanceStepUpNotification(
            Member member,
            List<String> accountIds) {
        return member.triggerBalanceStepUpNotificationBlocking(accountIds);
    }
}
