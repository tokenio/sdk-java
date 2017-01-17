package io.token;

import static io.token.asserts.MemberAssertion.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionThrownBy;

import io.token.proto.common.security.SecurityProtos.Key.Level;
import io.token.security.crypto.CryptoType;
import io.token.security.keystore.SecretKeyPair;
import io.token.util.Util;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;

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
        SecretKeyPair newKey = rule.token().createKey(CryptoType.EDDSA);
        member.approveKey(newKey, Level.PRIVILEGED);

        Member loggedIn = rule.token().login(member.memberId(), newKey);
        assertThat(loggedIn)
                .hasUsernames(member.usernames())
                .hasNKeys(2);
    }

    @Test
    public void loginMember_notAuthorized() {
        Member member = rule.member();
        SecretKeyPair newKey = rule.token().createKey(CryptoType.EDDSA); // Key is not approved.
        assertThatExceptionThrownBy(() -> {
            rule.token().login(member.memberId(), newKey);
            return 0;
        }).hasMessageContaining("PERMISSION_DENIED");
    }

    @Test
    public void addKey() {
        SecretKeyPair key2 = rule.token().createKey(CryptoType.EDDSA);
        SecretKeyPair key3 = rule.token().createKey(CryptoType.EDDSA);

        Member member = rule.member();
        member.approveKey(key2, Level.STANDARD);
        member.approveKey(key3, Level.PRIVILEGED);

        assertThat(member)
                .hasOneUsername()
                .hasNKeys(3)
                .hasKey(key2.id())
                .hasKey(key3.id());
    }

    @Test
    public void removeKey() {
        Member member = rule.member();

        SecretKeyPair key = rule.token().createKey(CryptoType.EDDSA);

        member.approveKey(key, Level.STANDARD);
        assertThat(member)
                .hasNKeys(2)
                .hasKey(key.id());

        member.removeKey(key.id());
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
