package io.token;

import static io.token.asserts.MemberAssertion.assertThat;

import io.token.proto.common.security.SecurityProtos.Key;
import io.token.util.Util;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;

public class MemberRegistrationTest {
    @Rule public final TokenRule rule = new TokenRule();
    private final Token token = rule.token();

    @Test
    public void createMember() {
        String username = Util.generateNonce();
        Member member = token.createMember(username);
        assertThat(member)
                .hasUsername(username)
                .hasNKeys(3);
    }

    @Test
    public void loginMember() {
        String username = Util.generateNonce();
        Member member = token.createMember(username);
        Member loggedIn = token.login(member.memberId());
        assertThat(loggedIn)
                .hasUsernames(member.usernames())
                .hasNKeys(3);
    }

    @Test
    public void provisionDevice() {
        String username = Util.generateNonce();

        Token firstDevice = rule.token();
        Member member = firstDevice.createMember(username);

        Token secondDevice = rule.token();
        DeviceInfo deviceInfo = secondDevice.provisionDevice(member.firstUsername());
        member.approveKeys(deviceInfo.getKeys());

        Member loggedIn = secondDevice.login(deviceInfo.getMemberId());
        assertThat(loggedIn)
                .hasUsernames(member.usernames())
                .hasNKeys(6);
    }

    @Test
    public void addUsername() {
        String username1 = Util.generateNonce();
        String username2 = Util.generateNonce();
        String username3 = Util.generateNonce();

        Member member = rule.token().createMember(username1);
        member.addUsername(username2);
        member.addUsername(username3);

        assertThat(member)
                .hasUsernames(username1, username2, username3)
                .hasNKeys(3);
    }

    @Test
    public void removeUsername() {
        String username1 = Util.generateNonce();
        String username2 = Util.generateNonce();

        Member member = rule.token().createMember(username1);

        member.addUsername(username2);
        assertThat(member).hasUsernames(username1, username2);

        member.removeUsername(username2);
        assertThat(member)
                .hasUsernames(username1)
                .hasNKeys(3);
    }

    @Test
    public void usernameDoesNotExist() {
        Assertions
                .assertThat(rule.token().usernameExists("john" + Util.generateNonce()))
                .isFalse();
    }

    @Test
    public void usernameExists() {
        String username = "john-" + Util.generateNonce();
        rule.token().createMember(username);
        Assertions.assertThat(rule.token().usernameExists(username)).isTrue();
    }
}
