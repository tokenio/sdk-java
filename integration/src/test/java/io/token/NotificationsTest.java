package io.token;

import static io.token.proto.common.notification.NotificationProtos.Notification.Status.DELIVERED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.util.concurrent.Uninterruptibles;
import io.token.common.TokenRule;
import io.token.proto.PagedList;
import io.token.proto.ProtoJson;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.proto.common.account.AccountProtos.AccountRoute;
import io.token.proto.common.account.AccountProtos.PlaintextBankAuthorization;
import io.token.proto.common.notification.NotificationProtos.Notification;
import io.token.proto.common.notification.NotificationProtos.Notification.Status;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.security.SecurityProtos.Key.Level;
import io.token.proto.common.security.SecurityProtos.SealedMessage;
import io.token.proto.common.subscriber.SubscriberProtos.Subscriber;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.Destination;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.Destination.TokenDestination;
import io.token.security.sealed.NoopSealedMessageEncrypter;
import io.token.testing.sample.Sample;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.ThrowableAssert;
import org.assertj.core.api.iterable.Extractor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class NotificationsTest {
    private static final int NOTIFICATION_TIMEOUT_MS = 5000;
    private static final String NOTIFICATION_TARGET =
            "0F7BF07748A12DE0C2393FD3731BFEB1484693DFA47A5C9614428BDF724548CD";
    private static final String TEST = "TEST"; // Test platform

    @Rule public TokenRule rule = new TokenRule();

    private final Account payerAccount = rule.account();
    private final Member payer = payerAccount.member();
    private final Member payee = rule.member();
    private BankAuthorization authorization;

    @Before
    public void setup() {
        String checking = ProtoJson.toJson(PlaintextBankAuthorization.newBuilder()
                .setAccountName("Checking")
                .setAccount(AccountRoute.newBuilder()
                        .setBic("irontest")
                        .setAccount("iban:checking"))
                .build());

        String saving = ProtoJson.toJson(PlaintextBankAuthorization.newBuilder()
                .setAccountName("Saving")
                .setAccount(AccountRoute.newBuilder()
                        .setBic("irontest")
                        .setAccount("iban:savings"))
                .build());

        NoopSealedMessageEncrypter encrypter = new NoopSealedMessageEncrypter();

        List<SealedMessage> accounts = Arrays.asList(
                encrypter.encrypt(checking),
                encrypter.encrypt(saving));
        authorization = BankAuthorization.newBuilder()
                .setBankId("BofA")
                .addAllAccounts(accounts)
                .build();
    }

    @Test
    public void testSubscribers() {
        final Subscriber subscriber = payer.subscribeToNotifications(
                "token",
                Sample.handlerInstructions(NOTIFICATION_TARGET, TEST));
        NotifyStatus res = rule.token().notifyLinkAccounts(
                payer.firstUsername(),
                authorization);
        assertThat(res).isEqualTo(NotifyStatus.ACCEPTED);
        List<Subscriber> subscriberList = payer.getSubscribers();
        assertThat(subscriberList.size()).isEqualTo(1);
        waitUntil(new Runnable() {
            public void run() {
                assertThat(payer.getNotifications(null, 100).getList().size())
                        .isGreaterThan(0);
            }
        });
        payer.unsubscribeFromNotifications(subscriber.getId());

        List<Subscriber> subscriberList2 = payer.getSubscribers();
        assertThat(subscriberList2.size()).isEqualTo(0);
        waitUntil(new Runnable() {
            public void run() {
                assertThat(payer.getNotifications(null, 100).getList().size())
                        .isEqualTo(0);
            }
        });
    }

    @Test
    public void testHandlerSubscriber() {
        payer.subscribeToNotifications("iron", new HashMap<String, String>());
        List<Subscriber> subscriberList = payer.getSubscribers();
        rule.token().notifyLinkAccounts(
                payer.firstUsername(),
                authorization);
        assertThat(subscriberList.size()).isEqualTo(1);
        waitUntil(new Runnable() {
            @Override
            public void run() {
                assertThat(payer.getNotifications(null, 100).getList().size())
                        .isGreaterThan(0);
            }
        });
    }

    @Test
    public void testHandlerSubscriberInstructions() {
        Map<String, String> instructionsBank = new HashMap<>();
        instructionsBank.put("sampleInstruction", "value");
        payer.subscribeToNotifications("iron", instructionsBank);
        List<Subscriber> subscriberList = payer.getSubscribers();
        rule.token().notifyLinkAccounts(
                payer.firstUsername(),
                authorization);
        assertThat(subscriberList.size()).isEqualTo(1);
        waitUntil(new Runnable() {
            @Override
            public void run() {
                assertThat(payer.getNotifications(null, 100).getList().size())
                        .isGreaterThan(0);
            }
        });
    }

    @Test
    public void getSubscriber() {
        Subscriber subscriber = payer.subscribeToNotifications("token", Sample
                .handlerInstructions(NOTIFICATION_TARGET, TEST));

        Subscriber subscriber2 = payer.getSubscriber(subscriber.getId());
        assertThat(subscriber).isEqualTo(subscriber2);
    }

    @Test
    public void triggerStepUpTransferNotification() {
        payer.subscribeToNotifications(
                "token",
                Sample.handlerInstructions(NOTIFICATION_TARGET, TEST));

        Token token = payer.createTransferToken(56, "USD")
                .setAccountId(payerAccount.id())
                .setRedeemerUsername(payee.firstUsername())
                .execute();

        TokenOperationResult res = payer.endorseToken(token, Key.Level.LOW);
        assertThat(res.getStatus())
                .isEqualTo(TokenOperationResult.Status.MORE_SIGNATURES_NEEDED);
        waitUntil(new Runnable() {
            @Override
            public void run() {
                assertThat(payer.getNotifications(null, 100).getList())
                        .hasSize(1)
                        .extracting(new NotificationStatusExtractor())
                        .containsExactly(DELIVERED);
            }
        });
    }

    @Test
    public void triggerStepUpTransferNotification_twoSubscribers() {
        payer.subscribeToNotifications("token", Sample
                .handlerInstructions(NOTIFICATION_TARGET, TEST));
        payer.subscribeToNotifications("token", Sample
                .handlerInstructions(NOTIFICATION_TARGET + "1", TEST));

        Token token = payer.createTransferToken(56, "USD")
                .setAccountId(payerAccount.id())
                .setRedeemerUsername(payee.firstUsername())
                .execute();
        Token token2 = payer.createTransferToken(56, "USD")
                .setAccountId(payerAccount.id())
                .setRedeemerUsername(payee.firstUsername())
                .execute();

        TokenOperationResult res = payer.endorseToken(token, Key.Level.LOW);
        TokenOperationResult res2 = payer.endorseToken(token2, Key.Level.LOW);
        assertThat(res.getStatus()).isEqualTo(TokenOperationResult.Status.MORE_SIGNATURES_NEEDED);
        assertThat(res2.getStatus()).isEqualTo(TokenOperationResult.Status.MORE_SIGNATURES_NEEDED);

        // 2 subscribers, 2 step ups
        waitUntil(new Runnable() {
            public void run() {
                assertThat(payer.getNotifications(null, 100).getList())
                        .extracting(new NotificationStatusExtractor())
                        .containsExactly(DELIVERED, DELIVERED, DELIVERED, DELIVERED);
            }
        });
    }

    @Test
    public void triggerStepUpAccessNotification() {
        payer.subscribeToNotifications(
                "token",
                Sample.handlerInstructions(NOTIFICATION_TARGET, TEST));
        Token token = payer.createAccessToken(AccessTokenBuilder
                .create(payee.firstUsername())
                .forAllAccounts());

        TokenOperationResult res = payer.endorseToken(token, Key.Level.LOW);
        assertThat(res.getStatus()).isEqualTo(TokenOperationResult.Status.MORE_SIGNATURES_NEEDED);
        waitUntil(new Runnable() {
            public void run() {
                assertThat(payer.getNotifications(null, 100).getList())
                        .hasSize(1)
                        .extracting(new NotificationStatusExtractor())
                        .containsExactly(DELIVERED);
            }
        });
    }

    @Test
    public void triggerTransferProcessedNotification() {
        payer.subscribeToNotifications(
                "token",
                Sample.handlerInstructions(NOTIFICATION_TARGET, TEST));

        Token token = payer.createTransferToken(20, "USD")
                .setAccountId(payerAccount.id())
                .setRedeemerUsername(payee.firstUsername())
                .execute();

        Token token2 = payer.createTransferToken(20, "USD")
                .setAccountId(payerAccount.id())
                .setRedeemerUsername(payee.firstUsername())
                .execute();

        Token endorsed = payer.endorseToken(token, Key.Level.STANDARD).getToken();
        Token endorsed2 = payer.endorseToken(token2, Level.STANDARD).getToken();
        Destination destination = Destination.newBuilder()
                .setTokenDestination(TokenDestination.newBuilder()
                        .setMemberId(payer.memberId())
                        .setAccountId(payerAccount.id())
                        .build())
                .build();
        payee.redeemToken(endorsed, null, null, destination);
        payee.redeemToken(endorsed2, null, null, destination);

        waitUntil(new Runnable() {
            public void run() {
                assertThat(payer.getNotifications(null, 100).getList())
                        .extracting(new NotificationStatusExtractor())
                        .containsExactly(DELIVERED, DELIVERED);
            }
        });
    }

    @Test
    public void sendNotifications() {
        String username = payer.firstUsername();

        try (TokenIO newSdk = rule.newSdkInstance()) {
            DeviceInfo deviceInfo = newSdk.provisionDevice(username);

            payer.subscribeToNotifications(
                    "token",
                    Sample.handlerInstructions(NOTIFICATION_TARGET, TEST));
            NotifyStatus res1 = newSdk.notifyLinkAccounts(
                    username,
                    authorization);
            NotifyStatus res2 = newSdk.notifyAddKey(
                    username,
                    "Chrome 52.0",
                    deviceInfo.getKeys().get(0));
            NotifyStatus res3 = newSdk.notifyLinkAccountsAndAddKey(
                    username,
                    authorization,
                    "Chrome 52.0",
                    deviceInfo.getKeys().get(0));

            assertThat(res1).isEqualTo(NotifyStatus.ACCEPTED);
            assertThat(res2).isEqualTo(NotifyStatus.ACCEPTED);
            assertThat(res3).isEqualTo(NotifyStatus.ACCEPTED);
            newSdk.notifyAddKey(
                    username,
                    "Chrome 52.0",
                    deviceInfo.getKeys().get(0));

            waitUntil(new Runnable() {
                public void run() {
                    assertThat(payer.getNotifications(null, 100).getList())
                            .extracting(new NotificationStatusExtractor())
                            .containsExactly(DELIVERED, DELIVERED, DELIVERED, DELIVERED);
                }
            });
        }
    }

    @Test
    public void sendLinkAccounts() {
        payer.subscribeToNotifications(
                "token",
                Sample.handlerInstructions(NOTIFICATION_TARGET, TEST));

        rule.token().notifyLinkAccounts(
                payer.firstUsername(),
                authorization);

        waitUntil(new Runnable() {
            public void run() {
                assertThat(payer.getNotifications(null, 100).getList())
                        .extracting(new NotificationStatusExtractor())
                        .containsExactly(DELIVERED);
            }
        });
    }

    @Test
    public void sendLinkAccountsAndAddKey() {
        payer.subscribeToNotifications(
                "token",
                Sample.handlerInstructions(NOTIFICATION_TARGET, TEST));

        try (TokenIO newSdk = rule.newSdkInstance()) {
            DeviceInfo deviceInfo = newSdk.provisionDevice(payer.firstUsername());
            rule
                    .token()
                    .notifyLinkAccountsAndAddKey(
                            payer.firstUsername(),
                            authorization,
                            "Chrome",
                            deviceInfo.getKeys().get(0));

            waitUntil(new Runnable() {
                public void run() {
                    assertThat(payer.getNotifications(null, 100).getList())
                            .extracting(new NotificationStatusExtractor())
                            .containsExactly(DELIVERED);
                }
            });
        }
    }

    @Test
    public void getNotificationsEmpty() {
        assertThat(payer.getNotifications(null, 100).getList()).isEmpty();
    }

    @Test
    public void getNotificationFalse() {
        assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
            public void call() throws Throwable {
                payer.getNotification("123456789");
            }
        });
    }

    @Test
    public void getNotificationTrue() {
        payer.subscribeToNotifications(
                "token",
                Sample.handlerInstructions(NOTIFICATION_TARGET, TEST));

        rule.token().notifyLinkAccounts(
                payer.firstUsername(),
                authorization);

        waitUntil(new Runnable() {
            public void run() {
                List<Notification> notifications = payer.getNotifications(null, 5).getList();
                assertThat(notifications.size()).isGreaterThan(0);
                assertThat(payer.getNotification(notifications.get(0).getId()))
                        .isEqualTo(notifications.get(0));
            }
        });
    }

    @Test
    public void getNotificationsPaging() {
        payer.subscribeToNotifications(
                "token",
                Sample.handlerInstructions(NOTIFICATION_TARGET, TEST));

        rule.token().notifyLinkAccounts(
                payer.firstUsername(),
                authorization);
        rule.token().notifyLinkAccounts(
                payer.firstUsername(),
                authorization);
        rule.token().notifyLinkAccounts(
                payer.firstUsername(),
                authorization);
        rule.token().notifyLinkAccounts(
                payer.firstUsername(),
                authorization);

        waitUntil(new Runnable() {
            public void run() {
                PagedList<Notification, String> notifications = payer.getNotifications(null, 2);
                assertThat(notifications.getList().size()).isEqualTo(2);
                PagedList<Notification, String> notifications2 = payer.getNotifications(
                        notifications.getOffset(),
                        100);
                assertThat(notifications2.getList().size()).isEqualTo(2);
            }
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

    private static class NotificationStatusExtractor implements Extractor<Notification, Status> {
        @Override
        public Status extract(Notification notification) {
            return notification.getStatus();
        }
    }
}
