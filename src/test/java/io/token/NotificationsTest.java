package io.token;

import io.token.proto.ProtoJson;
import io.token.proto.common.account.AccountProtos;
import io.token.proto.common.subscriber.SubscriberProtos.Platform;
import io.token.proto.common.subscriber.SubscriberProtos.Subscriber;
import io.token.security.Crypto;
import io.token.security.SecretKey;
import io.token.util.Util;
import io.token.util.codec.ByteEncoding;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionThrownBy;

public class NotificationsTest {
    @Rule public TokenRule rule = new TokenRule();
    private final Member member = rule.member();

    @Test
    public void sendNotification() {
        SecretKey key = Crypto.generateSecretKey();
        String username = member.usernames().get(0);
        String target = "8E8E256A58DE0F62F4A427202DF8CB07C6BD644AFFE93210BC49B8E5F9402554000";
        Subscriber subscriber = member.subscribeToNotifications(target, Platform.IOS);

        byte[] checking = ProtoJson.toJson(AccountProtos.AccountLinkPayload.newBuilder()
                .setAccountName("Checking")
                .setAccountNumber("iban:checking")
                .build()).getBytes();

        byte[] saving = ProtoJson.toJson(AccountProtos.AccountLinkPayload.newBuilder()
                .setAccountName("Saving")
                .setAccountNumber("iban:saving")
                .build()).getBytes();

        List<String> accountLinkPayloads = Stream.of(checking, saving)
                .map(ByteEncoding::serialize)
                .collect(toList());

        rule.token().notifyLinkAccounts(username, "BofA", "Bank of America", accountLinkPayloads);
        rule.token().notifyAddKey(username, key.getPublicKey(), "Chrome 52.0");
        rule.token().notifyLinkAccountsAndAddKey(
                username,
                "BofA",
                "Bank of America",
                accountLinkPayloads,
                key.getPublicKey(),
                "Chrome 52.0");

        List<Subscriber> subscriberList = member.getSubscribers();
        assertThat(subscriberList.size()).isEqualTo(1);

        member.unsubscribeFromNotifications(subscriber.getId());


        List<Subscriber> subscriberList2 = member.getSubscribers();
        assertThat(subscriberList2.size()).isEqualTo(0);


        assertThatExceptionThrownBy(() -> {
            rule.token().notifyAddKey(username, key.getPublicKey(), "Chrome 52.0");
            return 0;
        }).hasMessageContaining("NOT_FOUND");
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
}
