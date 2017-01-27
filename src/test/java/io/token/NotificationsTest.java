package io.token;

import static io.token.proto.common.notification.NotificationProtos.Notification.Status.DELIVERED;
import static io.token.proto.common.subscriber.SubscriberProtos.Platform.TEST;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionThrownBy;

import com.google.common.util.concurrent.Uninterruptibles;
import io.token.proto.ProtoJson;
import io.token.proto.common.account.AccountProtos;
import io.token.proto.common.notification.NotificationProtos.Notification;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.security.SecurityProtos.Key.Level;
import io.token.proto.common.security.SecurityProtos.SealedMessage;
import io.token.proto.common.subscriber.SubscriberProtos.Subscriber;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.security.sealed.NoopSealedMessageEncrypter;
import io.token.util.Util;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class NotificationsTest {
    @Rule public TokenRule rule = new TokenRule();

    private static final String NOTIFICATION_TARGET
            = "0F7BF07748A12DE0C2393FD3731BFEB1484693DFA47A5C9614428BDF724548CD";
    private static final int NOTIFICATION_TIMEOUT_MS = 5000;

    private final Account payerAccount = rule.account();
    private final Member payer = payerAccount.member();
    private final Member payee = rule.member();
    private List<SealedMessage> accountLinkPayloads;

    @Before
    public void setup() {
        String checking = ProtoJson.toJson(AccountProtos.AccountLinkPayload.newBuilder()
                .setAccountName("Checking")
                .setAccountNumber("iban:checking")
                .build());

        String saving = ProtoJson.toJson(AccountProtos.AccountLinkPayload.newBuilder()
                .setAccountName("Saving")
                .setAccountNumber("iban:saving")
                .build());

        NoopSealedMessageEncrypter encrypter = new NoopSealedMessageEncrypter();

        accountLinkPayloads = Stream.of(checking, saving)
                .map(encrypter::encrypt)
                .collect(toList());
    }

    @Test
    public void testSubscribers() {
        final Subscriber subscriber = payer.subscribeToNotifications(NOTIFICATION_TARGET, TEST);
        NotifyStatus res = rule.token().notifyLinkAccounts(
                payer.firstUsername(),
                "BofA",
                "Bank of America",
                accountLinkPayloads);
        assertThat(res).isEqualTo(NotifyStatus.ACCEPTED);
        List<Subscriber> subscriberList = payer.getSubscribers();
        assertThat(subscriberList.size()).isEqualTo(1);
        waitUntil(() -> assertThat(payer.getNotifications().size())
                .isGreaterThan(0)
        );
        payer.unsubscribeFromNotifications(subscriber.getId());

        List<Subscriber> subscriberList2 = payer.getSubscribers();
        assertThat(subscriberList2.size()).isEqualTo(0);
        waitUntil(() -> assertThat(payer.getNotifications().size()).isEqualTo(0));
    }

    @Test
    public void getSubscriber() {
        String target = Util.generateNonce();
        Subscriber subscriber = payer.subscribeToNotifications(target, TEST);

        Subscriber subscriber2 = payer.getSubscriber(subscriber.getId());
        assertThat(subscriber).isEqualTo(subscriber2);
    }

    @Test
    public void triggerStepUpTransferNotification() {
        payer.subscribeToNotifications(NOTIFICATION_TARGET, TEST);

        Token token = payer.createToken(
                56,
                "USD",
                payerAccount.id(),
                payee.firstUsername(),
                null);

        TokenOperationResult res = payer.endorseToken(token, Key.Level.LOW);
        assertThat(res.getStatus()).isEqualTo(TokenOperationResult.Status.MORE_SIGNATURES_NEEDED);
        waitUntil(() -> assertThat(payer.getNotifications())
                .hasSize(1)
                .extracting(Notification::getStatus)
                .containsExactly(DELIVERED));
    }

    @Test
    public void triggerStepUpTransferNotification_twoSubscribers() {
        payer.subscribeToNotifications(NOTIFICATION_TARGET, TEST);
        payer.subscribeToNotifications(NOTIFICATION_TARGET + "1", TEST);

        Token token = payer.createToken(
                56,
                "USD",
                payerAccount.id(),
                payee.firstUsername(),
                null);
        Token token2 = payer.createToken(
                57,
                "USD",
                payerAccount.id(),
                payee.firstUsername(),
                null);

        TokenOperationResult res = payer.endorseToken(token, Key.Level.LOW);
        TokenOperationResult res2 = payer.endorseToken(token2, Key.Level.LOW);
        assertThat(res.getStatus()).isEqualTo(TokenOperationResult.Status.MORE_SIGNATURES_NEEDED);
        assertThat(res2.getStatus()).isEqualTo(TokenOperationResult.Status.MORE_SIGNATURES_NEEDED);

        // 2 subscribers, 2 step ups
        waitUntil(() -> assertThat(payer.getNotifications())
                .extracting(Notification::getStatus)
                .containsExactly(DELIVERED, DELIVERED, DELIVERED, DELIVERED));
    }

    @Test
    public void triggerStepUpAccessNotification() {
        payer.subscribeToNotifications(NOTIFICATION_TARGET, TEST);
        Token token = payer.createAccessToken(AccessTokenBuilder
                .create(payee.firstUsername())
                .forAllAccounts());

        TokenOperationResult res = payer.endorseToken(token, Key.Level.LOW);
        assertThat(res.getStatus()).isEqualTo(TokenOperationResult.Status.MORE_SIGNATURES_NEEDED);
        waitUntil(() -> assertThat(payer.getNotifications())
                .hasSize(1)
                .extracting(Notification::getStatus)
                .containsExactly(DELIVERED));
    }

    @Test
    public void triggerTransferProcessedNotification() {
        payer.subscribeToNotifications(NOTIFICATION_TARGET, TEST);
        Token token = payer.createToken(
                20,
                "USD",
                payerAccount.id(),
                payee.firstUsername(),
                null);
        Token t2 = payer.createToken(
                20,
                "USD",
                payerAccount.id(),
                payee.firstUsername(),
                null);
        Token endorsed = payer.endorseToken(token, Key.Level.STANDARD).getToken();
        Token endorsed2 = payer.endorseToken(t2, Level.STANDARD).getToken();
        payee.redeemToken(endorsed);
        payee.redeemToken(endorsed2);

        waitUntil(() -> assertThat(payer.getNotifications())
                .extracting(Notification::getStatus)
                .containsExactly(DELIVERED, DELIVERED));
    }

    @Test
    public void sendNotifications() {
        String username = payer.firstUsername();

        DeviceInfo deviceInfo = rule.token().provisionDevice(username);

        payer.subscribeToNotifications(NOTIFICATION_TARGET, TEST);
        NotifyStatus res1 = rule.token().notifyLinkAccounts(
                username,
                "BofA",
                "Bank of America",
                accountLinkPayloads);
        NotifyStatus res2 = rule.token().notifyAddKey(
                username,
                "Chrome 52.0",
                deviceInfo.getKeys().get(0));
        NotifyStatus res3 = rule.token().notifyLinkAccountsAndAddKey(
                username,
                "BofA",
                "Bank of America",
                accountLinkPayloads,
                "Chrome 52.0",
                deviceInfo.getKeys().get(0));

        assertThat(res1).isEqualTo(NotifyStatus.ACCEPTED);
        assertThat(res2).isEqualTo(NotifyStatus.ACCEPTED);
        assertThat(res3).isEqualTo(NotifyStatus.ACCEPTED);
        rule.token().notifyAddKey(
                username,
                "Chrome 52.0",
                deviceInfo.getKeys().get(0));

        waitUntil(() -> assertThat(payer.getNotifications())
                .extracting(Notification::getStatus)
                .containsExactly(DELIVERED, DELIVERED, DELIVERED, DELIVERED));
    }

    @Test
    public void sendLinkAccounts() {
        payer.subscribeToNotifications(NOTIFICATION_TARGET, TEST);

        rule.token().notifyLinkAccounts(
                payer.firstUsername(),
                "BofA",
                "Bank of America",
                accountLinkPayloads);

        waitUntil(() -> assertThat(payer.getNotifications())
                .extracting(Notification::getStatus)
                .containsExactly(DELIVERED));
    }

    @Test
    public void sendLinkAccountsAndAddKey() {
        payer.subscribeToNotifications(NOTIFICATION_TARGET, TEST);

        DeviceInfo deviceInfo = rule.token().provisionDevice(payer.firstUsername());
        rule
                .token()
                .notifyLinkAccountsAndAddKey(payer.firstUsername(),
                        "BofA",
                        "Bank of America",
                        accountLinkPayloads,
                        "Chrome",
                        deviceInfo.getKeys().get(0));

        waitUntil(() -> assertThat(payer.getNotifications())
                .extracting(Notification::getStatus)
                .containsExactly(DELIVERED));
    }

    @Test
    public void getNotificationsEmpty() {
        assertThat(payer.getNotifications()).isEmpty();
    }

    @Test
    public void getNotificationFalse() {
        assertThatExceptionThrownBy(() -> {
            payer.getNotification("123456789");
            return null;
        });
    }

    @Test
    public void getNotificationTrue() {
        payer.subscribeToNotifications(NOTIFICATION_TARGET, TEST);

        rule.token().notifyLinkAccounts(
                payer.firstUsername(),
                "BofA",
                "Bank of America",
                accountLinkPayloads);

        waitUntil(() -> {
            List<Notification> notifications = payer.getNotifications();
            assertThat(notifications.size()).isGreaterThan(0);
            assertThat(payer.getNotification(notifications.get(0).getId()))
                    .isEqualTo(notifications.get(0));
        });
    }

    private void waitUntil(Runnable function) {
        for (long waitTimeMs = 1, start = System.currentTimeMillis(); ; waitTimeMs *= 2) {
            try {
                function.run();
                return;
            } catch (AssertionError caughtError) {
                if (System.currentTimeMillis() - start < NOTIFICATION_TIMEOUT_MS) {
                    // Notification not delivered yet
                    Uninterruptibles.sleepUninterruptibly(waitTimeMs, TimeUnit.MILLISECONDS);
                } else {
                    throw caughtError;
                }
            }
        }
    }
}
