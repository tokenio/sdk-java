package io.token;

import io.token.proto.common.token.TokenProtos.Token;
import org.junit.Rule;
import org.junit.Test;

import static io.token.asserts.TokenAssertion.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

public class PaymentTokenTest {
    @Rule public TokenRule rule = new TokenRule();
    private final Account account = rule.account();

    @Test
    public void createToken() {
        Token token = account.createToken(100.0, "USD", "amazon", "book purchase");
        assertThat(token)
                .hasPayer(account.getMember())
                .hasRedeemerAlias("amazon")
                .hasAmount(100.0)
                .hasCurrency("USD")
                .hasNoSignatures();
    }

    @Test
    public void lookupToken() {
        Token token = account.createToken(100.0, "USD");
        assertThat(account.lookupToken(token.getId()))
                .hasPayer(account.getMember())
                .hasAmount(100.0)
                .hasCurrency("USD")
                .hasNoSignatures();
    }

    @Test
    public void lookupTokens() {
        Token token1 = account.createToken(123.45, "EUR");
        Token token2 = account.createToken(678.90, "USD");
        Token token3 = account.createToken(100.99, "USD");

        assertThat(account.lookupTokens(0, 100))
                .hasSize(3)
                .containsOnly(token1, token2, token3);
    }

    @Test
    public void endorseToken() {
        Token token = account.createToken(100.0, "USD");
        token = account.endorseToken(token);

        assertThat(token)
                .hasNSignatures(2)
                .isEndorsedBy(account.getMember())
                .hasPayer(account.getMember())
                .hasAmount(100.0)
                .hasCurrency("USD");
    }

    @Test
    public void declineToken() {
        Token token = account.createToken(100.0, "USD");
        token = account.declineToken(token);

        assertThat(token)
                .hasNSignatures(2)
                .isDeclinedBy(account.getMember())
                .hasPayer(account.getMember())
                .hasAmount(100.0)
                .hasCurrency("USD");
    }

    @Test
    public void revokeToken() {
        Token token = account.createToken(100.0, "USD");
        token = account.endorseToken(token);
        token = account.revokeToken(token);

        assertThat(token)
                .hasNSignatures(4)
                .isEndorsedBy(account.getMember())
                .isRevokedBy(account.getMember())
                .hasPayer(account.getMember())
                .hasAmount(100.0)
                .hasCurrency("USD");
    }
}
