package io.token;

import io.token.security.Crypto;
import io.token.security.SecretKey;
import io.token.util.Util;
import org.junit.Test;

import static io.token.MemberAssertion.assertThat;

public class TokenTest {
    private final Token token = Token.builder()
            .hostName("localhost")
            .port(9000)
            .build();

    @Test
    public void createMember() {
        String alias = "alexey-" + Util.generateNonce();
        Member member = token.createMember(alias);
        assertThat(member)
                .hasAlias(alias)
                .hasNKeys(1);
    }

    @Test
    public void loginMember() {
        String alias = "alexey-" + Util.generateNonce();
        Member member = token.createMember(alias);

        Member loggedIn = token.login(member.getMemberId(), member.getKey());
        assertThat(loggedIn)
                .hasAlias(alias)
                .hasNKeys(1);
    }

    @Test
    public void addKey() {
        String alias = "alexey-" + Util.generateNonce();
        SecretKey key2 = Crypto.generateSecretKey();
        SecretKey key3 = Crypto.generateSecretKey();

        Member member = token.createMember(alias);
        member.approveKey(key2.getPublicKey(), 1);
        member.approveKey(key3.getPublicKey(), 0);

        assertThat(member)
                .hasAlias(alias)
                .hasNKeys(3)
                .hasKey(key2.getPublicKey())
                .hasKey(key3.getPublicKey());
    }

    @Test
    public void addAlias() {
        String alias1 = "alexey-" + Util.generateNonce();
        String alias2 = "alex-" + Util.generateNonce();
        String alias3 = "ak-" + Util.generateNonce();

        Member member = token.createMember(alias1);
        member.addAlias(alias2);
        member.addAlias(alias3);

        assertThat(member)
                .hasAliases(alias1, alias2, alias3)
                .hasNKeys(1);
    }
}
