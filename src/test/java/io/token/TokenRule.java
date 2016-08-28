package io.token;

import io.token.util.Util;
import org.junit.rules.ExternalResource;

public class TokenRule extends ExternalResource {
    private final Token token = Token.builder()
            .hostName("localhost")
            .port(9000)
            .build();

    public Token token() {
       return token;
    }

    public Member member() {
        String alias = "alexey-" + Util.generateNonce();
        return token.createMember(alias);
    }

    public Account account() {
        Member member = member();
        return member.account();
    }

    @Override
    protected void before() throws Throwable {
        super.before();
    }
}
