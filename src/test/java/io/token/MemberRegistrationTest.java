package io.token;

import io.token.proto.common.security.SecurityProtos.Key.Level;
import io.token.security.Crypto;
import io.token.security.SecretKey;
import io.token.util.Util;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;

import static io.token.asserts.MemberAssertion.assertThat;

public class MemberRegistrationTest {
    @Rule public TokenRule rule = new TokenRule();

    @Test
    public void createMember() {
        String username = "alexey-" + Util.generateNonce();
        Member member = rule.token().createMember(username);
        assertThat(member)
                .hasUsername(username)
                .hasOneKey();
    }

    @Test
    public void loginMember() {
        Member member = rule.member();
        Member loggedIn = rule.token().login(member.memberId(), member.key());
        assertThat(loggedIn)
                .hasUsernames(member.usernames())
                .hasOneKey();
    }

    @Test
    public void addKey() {
        SecretKey key2 = Crypto.generateSecretKey();
        SecretKey key3 = Crypto.generateSecretKey();

        Member member = rule.member();
        member.approveKey(key2.getPublicKey(), Level.STANDARD);
        member.approveKey(key3.getPublicKey(), Level.PRIVILEGED);

        assertThat(member)
                .hasOneUsername()
                .hasNKeys(3)
                .hasKey(key2.getPublicKey())
                .hasKey(key3.getPublicKey());
    }

    @Test
    public void removeKey() {
        Member member = rule.member();

        SecretKey key2 = Crypto.generateSecretKey();
        member.approveKey(key2.getPublicKey(), Level.STANDARD);
        assertThat(member)
                .hasNKeys(2)
                .hasKey(key2.getPublicKey());

        member.removeKey(key2.getId());
        assertThat(member)
                .hasOneUsername()
                .hasOneKey();
    }

    @Test
    public void addUsername() {
        String username1 = "alexey-" + Util.generateNonce();
        String username2 = "alex-" + Util.generateNonce();
        String username3 = "ak-" + Util.generateNonce();

        Member member = rule.token().createMember(username1);
        member.addUsername(username2);
        member.addUsername(username3);

        assertThat(member)
                .hasUsernames(username1, username2, username3)
                .hasOneKey();
    }

    @Test
    public void removeUsername() {
        String username1 = "alexey-" + Util.generateNonce();
        String username2 = "alex-" + Util.generateNonce();

        Member member = rule.token().createMember(username1);

        member.addUsername(username2);
        assertThat(member).hasUsernames(username1, username2);

        member.removeUsername(username2);
        assertThat(member)
                .hasUsernames(username1)
                .hasOneKey();
    }

    @Test
    public void usernameDoesNotExist() {
        Assertions.assertThat(rule.token().usernameExists("john" + Util.generateNonce())).isFalse();
    }

    @Test
    public void usernameExists() {
        String username = "john-" + Util.generateNonce();
        rule.token().createMember(username);
        Assertions.assertThat(rule.token().usernameExists(username)).isTrue();
    }
}
