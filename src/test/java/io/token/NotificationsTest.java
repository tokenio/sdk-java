package io.token;

import com.google.common.io.BaseEncoding;
import com.google.protobuf.ByteString;
import io.token.asserts.AccountAssertion;
import io.token.proto.ProtoJson;
import io.token.proto.common.account.AccountProtos.AccountLinkPayload;
import io.token.proto.common.device.DeviceProtos.Platform;
import io.token.security.Crypto;
import io.token.security.SecretKey;
import io.token.util.Util;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionThrownBy;

public class NotificationsTest {
    @Rule public TokenRule rule = new TokenRule();
    private final Member member = rule.member();

    @Test
    public void sendANotification() {
        SecretKey key = Crypto.generateSecretKey();
        String alias = member.getAliases().get(0);
        String bankId = "bank-id";
        List<String> tags = new ArrayList<>();
        tags.add("my iphone");
        String notificationUri = Util.generateNonce();
        member.subscribeDevice("Token", notificationUri, Platform.TEST, tags);


        byte[] data = ProtoJson.toJson(AccountLinkPayload.newBuilder()
                        .setAlias(alias)
                        .addAccounts(AccountLinkPayload.NamedAccount.newBuilder()
                                .setName("Checking")
                                .setAccountNumber("iban:checking"))
                        .addAccounts(AccountLinkPayload.NamedAccount.newBuilder()
                                .setName("Savings")
                                .setAccountNumber("iban:savings"))
                        .build()).getBytes();
        String accountLinkPayload = BaseEncoding.base64().encode(data);

        rule.token().notifyLinkAccounts(alias, "bank-id", accountLinkPayload);
        rule.token().notifyAddKey(alias, key.getPublicKey(), tags);
        rule.token().notifyLinkAccountsAndAddKey(alias, "bank-id", accountLinkPayload, key.getPublicKey(),
                tags);

        member.unsubscribeDevice("Token", notificationUri);

        assertThatExceptionThrownBy(() -> {
            rule.token().notifyAddKey(alias, key.getPublicKey(), tags);
            return 0;
        }).hasMessageContaining("NOT_FOUND");
    }
}
