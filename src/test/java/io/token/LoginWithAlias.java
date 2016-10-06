package io.token;

import io.token.proto.common.security.SecurityProtos;
import io.token.security.Crypto;
import io.token.security.SecretKey;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionThrownBy;

public class LoginWithAlias {
    @Rule public TokenRule rule = new TokenRule();
    private final Member member = rule.member();

    @Test
    public void loginWithAlias() {
        SecretKey key = Crypto.generateSecretKey();
        String alias = member.getAliases().get(0);

        assertThatExceptionThrownBy(() -> {
            rule.token().loginWithAlias(alias, key);
            return 0;
        }).hasMessageContaining("INTERNAL");

        member.approveKey(key.getPublicKey(), SecurityProtos.Key.Level.PRIVILEGED);
        Member loggedIn = rule.token().loginWithAlias(alias, key);
        assertThat(loggedIn.getAliases().size()).isEqualTo(1);
    }
}
