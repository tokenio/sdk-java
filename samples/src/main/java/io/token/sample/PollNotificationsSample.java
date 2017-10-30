package io.token.sample;

import static io.token.util.Util.generateNonce;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.PagedList;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.notification.NotificationProtos.Notification;
import io.token.proto.common.notification.NotificationProtos.NotifyBody.BodyCase;
import io.token.proto.common.subscriber.SubscriberProtos.Subscriber;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class PollNotificationsSample {
    /**
     * Creates and returns a new token member,
     * subscribed to notifications via a fake bank.
     *
     * @param tokenIO SDK client
     * @return a new Member instance
     */
    public static Member createMember(TokenIO tokenIO) {
        // Token members "in the real world" are set up to receive notifications:
        // The Token mobile app, having created a new member, subscribes to
        // notifications so they're delivered to the mobile device.
        //
        // When we create a member via createMember for testing, it's not
        // set up to receive notifications. If we try to send a notification
        // to this member, we would get a NO_SUBSCRIBERS error.
        // We set up a fake bank subscription for testing
        // (but we wouldn't do this in production for "real-world" members).
        Alias alias = Alias.newBuilder()
                .setType(Alias.Type.EMAIL)
                .setValue("test-" + generateNonce().toLowerCase() + "+noverify@example.com")
                .build();
        Member member = tokenIO.createMember(alias);
        Subscriber sub = member.subscribeToNotifications("iron");
        return member;
    }

    /**
     * Poll for notifications.
     *
     * @param member Whose notifications to poll for
     * @return a notification, maybe
     */
    public static Optional<Notification> poll(Member member) {
        for (int retries = 0; retries < 5; retries++) {
            // getNotifications doc extract start:
            PagedList<Notification, String> pagedList =
                    member.getNotifications(null, 10);
            List<Notification> notifications = pagedList.getList();
            if (!notifications.isEmpty()) {
                Notification notification = notifications.get(0);
                switch (BodyCase.valueOf(notification.getContent().getType())) {
                    case PAYMENT_REQUEST:
                        System.out.printf("Payment Request: %s", notification);
                        break;
                    default:
                        System.out.printf("Got notification: %s", notification);
                        break;
                }
                return Optional.of(notification);
            }
            // getNotifications doc extract end
            try {
                System.out.printf("Don't see notifications yet. Sleeping...");
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ie) {
                throw new RuntimeException(ie);
            }
        }
        // We waited a few seconds and still don't see any notification. Give up.
        return Optional.empty();
    }
}
