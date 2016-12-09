package io.token;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.util.concurrent.Uninterruptibles;
import io.token.proto.ProtoJson;
import io.token.proto.common.account.AccountProtos;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.security.SecurityProtos;
import io.token.proto.common.security.SecurityProtos.SealedMessage;
import io.token.proto.common.subscriber.SubscriberProtos.Platform;
import io.token.proto.common.subscriber.SubscriberProtos.Subscriber;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.security.Keys;
import io.token.security.Signer;
import io.token.security.cipher.noop.NoopCipher;
import io.token.security.testing.KeystoreTestRule;
import io.token.util.Util;

import java.security.PublicKey;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class NotificationsTest {
    @Rule public TokenRule rule = new TokenRule();
    @Rule public KeystoreTestRule keystoreRule = new KeystoreTestRule();
    private final Account payerAccount = rule.account();
    private final Account payeeAccount = rule.account();
    private final Member member = payerAccount.member();
    private final Member payee = payeeAccount.member();
    private List<SealedMessage> accountLinkPayloads;

    @Before
    public void setup() {
        NoopCipher cipher = new NoopCipher();

        String checking = ProtoJson.toJson(AccountProtos.AccountLinkPayload.newBuilder()
                .setAccountName("Checking")
                .setAccountNumber("iban:checking")
                .build());

        String saving = ProtoJson.toJson(AccountProtos.AccountLinkPayload.newBuilder()
                .setAccountName("Saving")
                .setAccountNumber("iban:saving")
                .build());

        accountLinkPayloads = Stream.of(checking, saving)
                .map(cipher::encrypt)
                .collect(toList());
    }

    @Test
    public void sendNotification() {
        PublicKey key = keystoreRule.getSigningKey().getPublic();
        byte[] encodedKey = Keys.encodeKey(key);
        String username = member.usernames().get(0);
        String target = "0F7BF07748A12DE0C2393FD3731BFEB1484693DFA47A5C9614428BDF724548CD00";
        Subscriber subscriber = member.subscribeToNotifications(target, Platform.IOS);

        rule.token().notifyLinkAccounts(username, "BofA", "Bank of America", accountLinkPayloads);
        rule.token().notifyAddKey(username, encodedKey, "Chrome 52.0");
        NotifyStatus res = rule.token().notifyLinkAccountsAndAddKey(
                username,
                "BofA",
                "Bank of America",
                accountLinkPayloads,
                encodedKey,
                "Chrome 52.0");
        assertThat(res).isEqualTo(NotifyStatus.ACCEPTED);
        List<Subscriber> subscriberList = member.getSubscribers();
        assertThat(subscriberList.size()).isEqualTo(1);

        member.unsubscribeFromNotifications(subscriber.getId());


        List<Subscriber> subscriberList2 = member.getSubscribers();
        assertThat(subscriberList2.size()).isEqualTo(0);

        rule.token().notifyAddKey(username, encodedKey, "Chrome 52.0");
    }

    @Test
    public void sendStepUpNotification() {
        PublicKey key = keystoreRule.getSigningKey().getPublic();
        Signer signer = keystoreRule.getSecretKeyStore().createSigner();
        Account payerAccount = rule.account();
        Member member = payerAccount.member();
        String target = "17D6F20C68314B508D71FC382162479746F0C41B9144FAE592162F43175444F4";
        member.subscribeToNotifications(target, Platform.IOS);
        member.approveKey(Keys.encodeKey(key), SecurityProtos.Key.Level.LOW);
        Member memberLow = rule.token().login(member.memberId(), signer);
        Token t = memberLow.createToken(56, "USD", payerAccount.id(), payee.firstUsername(), null);

        TokenOperationResult res = memberLow.endorseToken(t);
        assertThat(res.getStatus() == TokenOperationResult.Status.MORE_SIGNATURES_NEEDED);
    }

    @Ignore("Flaky test")
    @Test
    public void sendStepUpAccessNotification() {
        PublicKey key = keystoreRule.getSigningKey().getPublic();
        Signer signer = keystoreRule.getSecretKeyStore().createSigner();
        Account payerAccount = rule.account();
        Member member = payerAccount.member();
        String target = "17D6F20C68314B508D71FC382162479746F0C41B9144FAE592162F43175444F4";
        member.subscribeToNotifications(target, Platform.IOS);
        member.approveKey(Keys.encodeKey(key), SecurityProtos.Key.Level.LOW);
        Member memberLow = rule.token().login(member.memberId(), signer);
        Token t = memberLow.createAccessToken(AccessTokenBuilder
                .create(payee.firstUsername())
                .forAllAccounts());

        TokenOperationResult res = memberLow.endorseToken(t);
        assertThat(res.getStatus() == TokenOperationResult.Status.MORE_SIGNATURES_NEEDED);
    }

    @Test
    public void sendTransferProcessedNotification() {
        Account payerAccount = rule.account();
        Member member = payerAccount.member();
        String target = "0F7BF07748A12DE0C2393FD3731BFEB1484693DFA47A5C9614428BDF724548CD";
        member.subscribeToNotifications(target, Platform.IOS);
        Token t = member.createToken(20, "USD", payerAccount.id(), payee.firstUsername(), null);

        Token endorsed = member.endorseToken(t).getToken();
        payee.redeemToken(endorsed);
    }

    @Test
    public void getSubscriber() {
        String target = Util.generateNonce();
        Subscriber subscriber = member.subscribeToNotifications(target, Platform.TEST);

        Subscriber subscriber2 = member.getSubscriber(subscriber.getId());
        assertThat(subscriber.getId()).isEqualTo(subscriber2.getId());
        assertThat(subscriber.getTarget()).isEqualTo(subscriber2.getTarget());
        assertThat(subscriber.getPlatform()).isEqualTo(subscriber2.getPlatform());
    }

    @Test
    public void deliveryTest_TransferProcessed() {
        Account payerAccount = rule.account();
        Member member = payerAccount.member();
        String target = "0F7BF07748A12DE0C2393FD3731BFEB1484693DFA47A5C9614428BDF724548CD";
        member.subscribeToNotifications(target, Platform.TEST);
        member.subscribeToNotifications(target + "1", Platform.TEST);
        Token t = member.createToken(20, "USD", payerAccount.id(), payee.firstUsername(), null);
        Token t2 = member.createToken(20, "USD", payerAccount.id(), payee.firstUsername(), null);
        Token endorsed = member.endorseToken(t).getToken();
        Token endorsed2 = member.endorseToken(t2).getToken();
        payee.redeemToken(endorsed);
        payee.redeemToken(endorsed2);
        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        assertThat(member.getNotifications().size()).isEqualTo(4); // 2 subscribers, 2 transfers
    }

    @Test
    public void deliveryTest_StepUp() {
        PublicKey key = keystoreRule.getSigningKey().getPublic();
        Signer signer = keystoreRule.getSecretKeyStore().createSigner();
        Account payerAccount = rule.account();
        Member member = payerAccount.member();
        String target = "17D6F20C68314B508D71FC382162479746F0C41B9144FAE592162F43175444F4";
        member.subscribeToNotifications(target, Platform.TEST);
        member.subscribeToNotifications(target + "1", Platform.TEST);
        member.approveKey(Keys.encodeKey(key), SecurityProtos.Key.Level.LOW);
        Member memberLow = rule.token().login(member.memberId(), signer);
        Token t = memberLow.createToken(56, "USD", payerAccount.id(), payee.firstUsername(), null);
        Token t2 = memberLow.createToken(57, "USD", payerAccount.id(), payee.firstUsername(), null);

        TokenOperationResult res = memberLow.endorseToken(t);
        TokenOperationResult res2 = memberLow.endorseToken(t2);
        assertThat(res.getStatus() == TokenOperationResult.Status.MORE_SIGNATURES_NEEDED);
        assertThat(res2.getStatus() == TokenOperationResult.Status.MORE_SIGNATURES_NEEDED);
        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        assertThat(member.getNotifications().size()).isEqualTo(4); // 2 subscribers, 2 step ups
    }

    @Test
    public void deliveryTest_LinkAccounts() {
        Account payerAccount = rule.account();
        Member member = payerAccount.member();
        String username = RandomStringUtils.randomAlphabetic(30);
        member.addUsername(username);
        String target = "17D6F20C68314B508D71FC382162479746F0C41B9144FAE592162F43175444F4";
        member.subscribeToNotifications(target, Platform.TEST);
        member.subscribeToNotifications(target + "1", Platform.TEST);

        rule.token().notifyLinkAccounts(username, "BofA", "Bank of America", accountLinkPayloads);
        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        assertThat(member.getNotifications().size()).isEqualTo(2); // 2 subscribers, 2 step ups
    }

    @Test
    public void getNotificationsEmpty() {
        Member member = payerAccount.member();
        assertThat(member.getNotifications().size()).isEqualTo(0);
    }
}
