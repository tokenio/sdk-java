package io.token;

import static io.token.proto.common.notification.NotificationProtos.Notification.Status.DELIVERED;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionThrownBy;

import com.google.common.util.concurrent.Uninterruptibles;
import io.token.proto.ProtoJson;
import io.token.proto.common.account.AccountProtos;
import io.token.proto.common.notification.NotificationProtos.Notification;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.security.SecurityProtos.Key.Level;
import io.token.proto.common.security.SecurityProtos.SealedMessage;
import io.token.proto.common.subscriber.SubscriberProtos.Platform;
import io.token.proto.common.subscriber.SubscriberProtos.Subscriber;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.security.crypto.CryptoType;
import io.token.security.keystore.SecretKeyPair;
import io.token.security.sealed.NoopSealedMessageEncrypter;
import io.token.util.Util;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class NotificationsTest {
    @Rule public TokenRule rule = new TokenRule();

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
        String username = payer.usernames().get(0);
        String target = "0F7BF07748A12DE0C2393FD3731BFEB1484693DFA47A5C9614428BDF724548CD00";

        final Subscriber subscriber = payer.subscribeToNotifications(target, Platform.IOS);
        NotifyStatus res = rule.token().notifyLinkAccounts(
                username,
                "BofA",
                "Bank of America",
                accountLinkPayloads);
        assertThat(res).isEqualTo(NotifyStatus.ACCEPTED);
        List<Subscriber> subscriberList = payer.getSubscribers();
        assertThat(subscriberList.size()).isEqualTo(1);
        assertThat(payer.getNotifications().size()).isGreaterThan(0);
        payer.unsubscribeFromNotifications(subscriber.getId());

        List<Subscriber> subscriberList2 = payer.getSubscribers();
        assertThat(subscriberList2.size()).isEqualTo(0);
        waitUntil(() -> assertThat(payer.getNotifications().size()).isEqualTo(0));
    }

    @Test
    public void getSubscriber() {
        String target = Util.generateNonce();
        Subscriber subscriber = payer.subscribeToNotifications(target, Platform.TEST);

        Subscriber subscriber2 = payer.getSubscriber(subscriber.getId());
        assertThat(subscriber).isEqualTo(subscriber2);
    }

    @Test
    public void triggerStepUpTransferNotification() {
        SecretKeyPair newKey = rule.token().createKey(CryptoType.EDDSA);
        payer.approveKey(newKey, Level.LOW);

        String target = "17D6F20C68314B508D71FC382162479746F0C41B9144FAE592162F43175444F4";
        payer.subscribeToNotifications(target, Platform.IOS);

        Member memberLow = rule.token().login(payer.memberId(), newKey);
        Token token = memberLow.createToken(
                56,
                "USD",
                payerAccount.id(),
                payee.firstUsername(),
                null);

        TokenOperationResult res = memberLow.endorseToken(token);
        assertThat(res.getStatus()).isEqualTo(TokenOperationResult.Status.MORE_SIGNATURES_NEEDED);
        waitUntil(() -> {
            assertThat(payer.getNotifications().size()).isEqualTo(1);
            assertThat(payer.getNotifications().get(0).getStatus()).isEqualTo((DELIVERED));
        });
    }

    @Test
    public void triggerStepUpTransferNotification_twoSubscribers() {
        SecretKeyPair newKey = rule.token().createKey(CryptoType.EDDSA);
        payer.approveKey(newKey, Level.LOW);

        String target = "17D6F20C68314B508D71FC382162479746F0C41B9144FAE592162F43175444F4";
        payer.subscribeToNotifications(target, Platform.TEST);
        payer.subscribeToNotifications(target + "1", Platform.TEST);
        Member memberLow = rule.token().login(payer.memberId(), newKey);

        Token token = memberLow.createToken(
                56,
                "USD",
                payerAccount.id(),
                payee.firstUsername(),
                null);
        Token token2 = memberLow.createToken(
                57,
                "USD",
                payerAccount.id(),
                payee.firstUsername(),
                null);

        TokenOperationResult res = memberLow.endorseToken(token);
        TokenOperationResult res2 = memberLow.endorseToken(token2);
        assertThat(res.getStatus()).isEqualTo(TokenOperationResult.Status.MORE_SIGNATURES_NEEDED);
        assertThat(res2.getStatus()).isEqualTo(TokenOperationResult.Status.MORE_SIGNATURES_NEEDED);
        waitUntil(() -> {
            assertThat(payer.getNotifications().size()).isEqualTo(4); // 2 subscribers, 2 step ups
            assertThat(payer.getNotifications().get(0).getStatus()).isEqualTo((DELIVERED));
            assertThat(payer.getNotifications().get(1).getStatus()).isEqualTo((DELIVERED));
            assertThat(payer.getNotifications().get(2).getStatus()).isEqualTo((DELIVERED));
            assertThat(payer.getNotifications().get(3).getStatus()).isEqualTo((DELIVERED));
        });
    }

    @Test
    public void triggerStepUpAccessNotification() {
        SecretKeyPair newKey = rule.token().createKey(CryptoType.EDDSA);
        payer.approveKey(newKey, Level.LOW);

        String target = "17D6F20C68314B508D71FC382162479746F0C41B9144FAE592162F43175444F4";
        payer.subscribeToNotifications(target, Platform.IOS);
        Member memberLow = rule.token().login(payer.memberId(), newKey);
        Token token = memberLow.createAccessToken(AccessTokenBuilder
                .create(payee.firstUsername())
                .forAllAccounts());

        TokenOperationResult res = memberLow.endorseToken(token);
        assertThat(res.getStatus()).isEqualTo(TokenOperationResult.Status.MORE_SIGNATURES_NEEDED);
        waitUntil(() -> {
            assertThat(payer.getNotifications().size()).isEqualTo(1);
            assertThat(payer.getNotifications().get(0).getStatus()).isEqualTo((DELIVERED));
        });
    }


    @Test
    public void triggerTransferProcessedNotification() {
        Account payerAccount = rule.account();
        Member member = payerAccount.member();
        String target = "0F7BF07748A12DE0C2393FD3731BFEB1484693DFA47A5C9614428BDF724548CD";
        member.subscribeToNotifications(target, Platform.TEST);
        member.subscribeToNotifications(target + "1", Platform.TEST);
        Token token = member.createToken(20, "USD", payerAccount.id(), payee.firstUsername(), null);
        Token t2 = member.createToken(20, "USD", payerAccount.id(), payee.firstUsername(), null);
        Token endorsed = member.endorseToken(token).getToken();
        Token endorsed2 = member.endorseToken(t2).getToken();
        payee.redeemToken(endorsed);
        payee.redeemToken(endorsed2);
        waitUntil(() -> {
            assertThat(member.getNotifications().size()).isEqualTo(4); // 2 subscribers, 2 transfers
            assertThat(member.getNotifications().get(0).getStatus()).isEqualTo((DELIVERED));
            assertThat(member.getNotifications().get(1).getStatus()).isEqualTo((DELIVERED));
            assertThat(member.getNotifications().get(2).getStatus()).isEqualTo((DELIVERED));
            assertThat(member.getNotifications().get(3).getStatus()).isEqualTo((DELIVERED));
        });
    }

    @Test
    public void sendNotifications() {
        SecretKeyPair newKey = rule.token().createKey(CryptoType.EDDSA);
        String username = payer.usernames().get(0);
        String target = "0F7BF07748A12DE0C2393FD3731BFEB1484693DFA47A5C9614428BDF724548CD00";

        payer.subscribeToNotifications(target, Platform.IOS);
        NotifyStatus res1 = rule.token().notifyLinkAccounts(
                username,
                "BofA",
                "Bank of America",
                accountLinkPayloads);
        NotifyStatus res2 = rule.token().notifyAddKey(
                username,
                "Chrome 52.0",
                newKey,
                Level.STANDARD);
        NotifyStatus res3 = rule.token().notifyLinkAccountsAndAddKey(
                username,
                "BofA",
                "Bank of America",
                accountLinkPayloads,
                "Chrome 52.0",
                newKey,
                Level.STANDARD);

        assertThat(res1).isEqualTo(NotifyStatus.ACCEPTED);
        assertThat(res2).isEqualTo(NotifyStatus.ACCEPTED);
        assertThat(res3).isEqualTo(NotifyStatus.ACCEPTED);
        rule.token().notifyAddKey(
                username,
                "Chrome 52.0",
                newKey,
                Level.STANDARD);
        waitUntil(() -> {
            assertThat(payer.getNotifications().size()).isEqualTo(4);
            assertThat(payer.getNotifications().get(0).getStatus()).isEqualTo((DELIVERED));
            assertThat(payer.getNotifications().get(1).getStatus()).isEqualTo((DELIVERED));
            assertThat(payer.getNotifications().get(2).getStatus()).isEqualTo((DELIVERED));
            assertThat(payer.getNotifications().get(3).getStatus()).isEqualTo((DELIVERED));
        });
    }

    @Test
    public void sendLinkAccounts() {
        Account payerAccount = rule.account();
        Member member = payerAccount.member();
        String username = RandomStringUtils.randomAlphabetic(30);
        member.addUsername(username);
        String target = "17D6F20C68314B508D71FC382162479746F0C41B9144FAE592162F43175444F4";
        member.subscribeToNotifications(target, Platform.TEST);
        member.subscribeToNotifications(target + "1", Platform.TEST);

        rule.token().notifyLinkAccounts(username, "BofA", "Bank of America", accountLinkPayloads);
        waitUntil(() -> {
            assertThat(member.getNotifications().size()).isEqualTo(2); // 2 subscribers, 2 step ups
            assertThat(member.getNotifications().get(0).getStatus()).isEqualTo((DELIVERED));
            assertThat(member.getNotifications().get(1).getStatus()).isEqualTo((DELIVERED));
        });
    }

    @Test
    public void sendLinkAccountsAndAddKey() {
        String username = RandomStringUtils.randomAlphabetic(30);
        payer.addUsername(username);
        String target = "17D6F20C68314B508D71FC382162479746F0C41B9144FAE592162F43175444F4";
        payer.subscribeToNotifications(target, Platform.TEST);
        payer.subscribeToNotifications(target + "1", Platform.TEST);

        SecretKeyPair newKey = rule.token().createKey(CryptoType.EDDSA);
        rule
                .token()
                .notifyLinkAccountsAndAddKey(username,
                        "BofA",
                        "Bank of America",
                        accountLinkPayloads,
                        "Chrome",
                        newKey,
                        Level.STANDARD);
        waitUntil(() -> {
            assertThat(payer.getNotifications().size()).isEqualTo(2); // 2 subs, 2 notifications
            assertThat(payer.getNotifications().get(0).getStatus()).isEqualTo((DELIVERED));
            assertThat(payer.getNotifications().get(1).getStatus()).isEqualTo((DELIVERED));
        });
    }

    @Test
    public void getNotificationsEmpty() {
        assertThat(payer.getNotifications().size()).isEqualTo(0);
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
        String username = RandomStringUtils.randomAlphabetic(30);
        payer.addUsername(username);
        String target = "17D6F20C68314B508D71FC382162479746F0C41B9144FAE592162F43175444F4";
        payer.subscribeToNotifications(target, Platform.TEST);
        rule.token().notifyLinkAccounts(username, "BofA", "Bank of America", accountLinkPayloads);

        waitUntil(() -> {
            List<Notification> notifications = payer.getNotifications();
            assertThat(notifications.size()).isGreaterThan(0);
            assertThat(payer.getNotification(notifications.get(0).getId()).getId()).isEqualTo(
                    notifications.get(0).getId());
        });
    }

    private void waitUntil(Runnable function) {
        long start = System.currentTimeMillis();
        for (int waitTimeMs = 1; System.currentTimeMillis() - start < NOTIFICATION_TIMEOUT_MS;
                waitTimeMs *= 2) {
            try {
                function.run();
                return;
            } catch (AssertionError caughtError) {
                // Notification not delivered yet
                Uninterruptibles.sleepUninterruptibly(waitTimeMs, TimeUnit.MILLISECONDS);
            }
        }
    }

}
