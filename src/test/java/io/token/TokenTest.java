package io.token;

import org.junit.Test;

public class TokenTest {
    private final Token token = Token.builder()
            .hostName("localhost")
            .port(9000)
            .build();

    @Test
    public void createMember() {
        Member member = token.createMember("alexey");
        System.out.println(member);
    }
}
