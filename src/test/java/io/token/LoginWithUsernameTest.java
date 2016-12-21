package io.token;

import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionThrownBy;

import io.token.security.SecretKeyStore;
import io.token.security.Signer;
import io.token.security.crypto.Crypto;
import io.token.security.crypto.EdDsaCrypto;
import io.token.security.keystore.InMemorySecretKeyStore;

import java.security.KeyPair;
import org.junit.Rule;
import org.junit.Test;

public class LoginWithUsernameTest {
    @Rule public TokenRule rule = new TokenRule();
    private final Member member = rule.member();
    private final Crypto crypto = EdDsaCrypto.getInstance();

    @Test
    public void loginWithUsername() {
        KeyPair keyPair = crypto.generateKeyPair();
        SecretKeyStore keyStore = new InMemorySecretKeyStore(keyPair);
        Signer signer = keyStore.createSigner();

        String username = member.usernames().get(0);

        assertThatExceptionThrownBy(() -> {
            rule.token().loginWithUsername(username, signer);
            return 0;
        }).hasMessageContaining("PERMISSION_DENIED");

        member.approveKey(keyPair.getPublic(), PRIVILEGED);
        Member loggedIn = rule.token().loginWithUsername(username, signer);
        assertThat(loggedIn.usernames().size()).isEqualTo(1);
    }
}
