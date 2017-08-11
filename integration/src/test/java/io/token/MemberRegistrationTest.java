package io.token;

import static io.token.asserts.MemberAssertion.assertThat;
import static io.token.proto.common.alias.AliasProtos.Alias.Type.EMAIL;
import static io.token.proto.common.testing.Sample.alias;

import io.token.common.TokenRule;
import io.token.proto.common.alias.AliasProtos.Alias;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;

public class MemberRegistrationTest {
    @Rule public final TokenRule rule = new TokenRule();

    @Test
    public void createMember() {
        Alias alias = alias();
        Member member = rule.token().createMember(alias);
        assertThat(member)
                .hasAlias(alias)
                .hasNKeys(3);
    }

    @Test
    public void createMember_noAlias() {
        Member member = rule.token().createMember();
        assertThat(member)
                .hasNAliases(0)
                .hasNKeys(3);
    }

    @Test
    public void loginMember() {
        Alias alias = alias();
        Member member = rule.token().createMember(alias);
        // TODO(PR-1005): change login to fetch aliases when the server stores aliases in plain text
        Member loggedIn = rule.token().login(member.memberId());

        // TODO(PR-1005): re-enable this when server sync is complete
        //        assertThat(loggedIn)
        //                .hasAliases(member.aliases())
        //                .hasNKeys(3);
    }

    @Test
    public void provisionDevice() {
        Alias alias = alias();

        Member member = rule.token().createMember(alias);

        try (TokenIO secondDevice = rule.newSdkInstance()) {
            DeviceInfo deviceInfo = secondDevice.provisionDevice(member.firstAlias());
            member.approveKeys(deviceInfo.getKeys());

            Member loggedIn = secondDevice.login(deviceInfo.getMemberId());

            // TODO(PR-1005): re-enable this when server sync is complete
            //            assertThat(loggedIn)
            //                    .hasAliases(member.aliases())
            //                    .hasNKeys(6);
        }
    }

    @Test
    public void addAlias() {
        Alias alias1 = alias();
        Alias alias2 = alias();
        Alias alias3 = alias();

        Member member = rule.token().createMember(alias1);
        member.addAlias(alias2);
        member.addAlias(alias3);

        assertThat(member)
                .hasAliases(alias1, alias2, alias3)
                .hasNKeys(3);
    }

    @Test
    public void removeAlias() {
        Alias alias1 = alias();
        Alias alias2 = alias();

        Member member = rule.token().createMember(alias1);

        member.addAlias(alias2);
        assertThat(member).hasAliases(alias1, alias2);

        member.removeAlias(alias2);
        assertThat(member)
                .hasAliases(alias1)
                .hasNKeys(3);
    }

    @Test
    public void aliasDoesNotExist() {
        Alias alias1 = Alias.newBuilder()
                .setValue("john@google.com")
                .setType(EMAIL)
                .build();
        Assertions
                .assertThat(rule.token().aliasExists(alias1))
                .isFalse();
    }

    @Test
    public void aliasExists() {
        Alias alias = alias();
        rule.token().createMember(alias);
        Assertions.assertThat(rule.token().aliasExists(alias)).isTrue();
    }
}
