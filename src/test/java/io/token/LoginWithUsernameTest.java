package io.token;

import io.token.proto.common.security.SecurityProtos;
import io.token.security.Crypto;
import io.token.security.SecretKey;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionThrownBy;

public class LoginWithUsernameTest {
    @Rule public TokenRule rule = new TokenRule();
    private final Member member = rule.member();

    @Test
    public void loginWithUsername() {
        SecretKey key = Crypto.generateSecretKey();
        String username = member.usernames().get(0);

        assertThatExceptionThrownBy(() -> {
            rule.token().loginWithUsername(username, key);
            return 0;
        }).hasMessageContaining("PERMISSION_DENIED");

        member.approveKey(key.getPublicKey(), SecurityProtos.Key.Level.PRIVILEGED);
        Member loggedIn = rule.token().loginWithUsername(username, key);
        assertThat(loggedIn.usernames().size()).isEqualTo(1);
    }
}
