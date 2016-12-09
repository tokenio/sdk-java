package io.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionThrownBy;

import io.token.proto.common.security.SecurityProtos;
import io.token.security.Keys;
import io.token.security.Signer;
import io.token.security.keystore.SecretKeyStore;
import io.token.security.testing.KeystoreTestRule;

import java.security.PublicKey;
import org.junit.Rule;
import org.junit.Test;

public class LoginWithUsernameTest {
    @Rule public TokenRule rule = new TokenRule();
    private final Member member = rule.member();
    @Rule public KeystoreTestRule keystoreRule = new KeystoreTestRule();

    @Test
    public void loginWithUsername() {
        SecretKeyStore keyStore = keystoreRule.getSecretKeyStore();
        PublicKey publicKey = keyStore.activeKey().getPublic();
        Signer signer = keyStore.createSigner();

        String username = member.usernames().get(0);

        assertThatExceptionThrownBy(() -> {
            rule.token().loginWithUsername(username, signer);
            return 0;
        }).hasMessageContaining("PERMISSION_DENIED");

        member.approveKey(Keys.encodeKey(publicKey), SecurityProtos.Key.Level.PRIVILEGED);
        Member loggedIn = rule.token().loginWithUsername(username, signer);
        assertThat(loggedIn.usernames().size()).isEqualTo(1);
    }
}
