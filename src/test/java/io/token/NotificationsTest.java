package io.token;

import io.token.proto.ProtoJson;
import io.token.proto.common.account.AccountProtos.AccountsLinkPayload;
import io.token.proto.common.device.DeviceProtos.Platform;
import io.token.security.Crypto;
import io.token.security.SecretKey;
import io.token.util.Util;
import io.token.util.codec.ByteEncoding;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatExceptionThrownBy;

public class NotificationsTest {
    @Rule public TokenRule rule = new TokenRule();
    private final Member member = rule.member();

    @Test
    public void sendNotification() {
        SecretKey key = Crypto.generateSecretKey();
        String alias = member.aliases().get(0);
        List<String> tags = new ArrayList<>();
        tags.add("my iphone");
        String notificationUri = Util.generateNonce();
        member.subscribeDevice("Token", notificationUri, Platform.TEST, tags);

        byte[] data = ProtoJson.toJson(AccountsLinkPayload.newBuilder()
                        .setAlias(alias)
                        .addAccounts(AccountsLinkPayload.NamedAccount.newBuilder()
                                .setName("Checking")
                                .setAccountNumber("iban:checking"))
                        .addAccounts(AccountsLinkPayload.NamedAccount.newBuilder()
                                .setName("Savings")
                                .setAccountNumber("iban:savings"))
                        .build()).getBytes();
        String accountLinkPayload = ByteEncoding.serialize(data);

        rule.token().notifyLinkAccounts(alias, "bank-id", accountLinkPayload);
        rule.token().notifyAddKey(alias, key.getPublicKey(), tags);
        rule.token().notifyLinkAccountsAndAddKey(
                alias,
                "bank-id",
                accountLinkPayload,
                key.getPublicKey(),
                tags);

        member.unsubscribeDevice("Token", notificationUri);

        assertThatExceptionThrownBy(() -> {
            rule.token().notifyAddKey(alias, key.getPublicKey(), tags);
            return 0;
        }).hasMessageContaining("NOT_FOUND");
    }
}
