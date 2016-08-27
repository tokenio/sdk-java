package io.token;

import org.junit.Rule;
import org.junit.Test;

public class PaymentTokenTest {
    @Rule public TokenRule rule = new TokenRule();
    private final Member member = rule.member();

    @Test
    public void createToken() {
    }
}
