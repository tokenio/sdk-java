package io.token;

import static io.token.asserts.MemberAssertion.assertThat;

import io.token.proto.common.security.SecurityProtos.Key.Level;
import io.token.security.Keys;
import io.token.util.Util;

import java.security.PublicKey;
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
        Member loggedIn = rule.token().login(member.memberId(), member.signer());
        assertThat(loggedIn)
                .hasUsernames(member.usernames())
                .hasOneKey();
    }

    @Test
    public void addKey() {
        PublicKey key2 = Keys.generateEdDsaKeyPair().getPublic();
        PublicKey key3 = Keys.generateEdDsaKeyPair().getPublic();

        byte[] encodedKey2 = Keys.encodeKey(key2);
        byte[] encodedKey3 = Keys.encodeKey(key3);

        Member member = rule.member();
        member.approveKey(encodedKey2, Level.STANDARD);
        member.approveKey(encodedKey3, Level.PRIVILEGED);

        assertThat(member)
                .hasOneUsername()
                .hasNKeys(3)
                .hasKey(encodedKey2)
                .hasKey(encodedKey3);
    }

    @Test
    public void removeKey() {
        Member member = rule.member();

        PublicKey publicKey = Keys.generateEdDsaKeyPair().getPublic();
        String keyId = Keys.keyIdFor(publicKey);
        byte[] encodedKey = Keys.encodeKey(publicKey);

        member.approveKey(encodedKey, Level.STANDARD);
        assertThat(member)
                .hasNKeys(2)
                .hasKey(encodedKey);

        member.removeKey(keyId);
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
