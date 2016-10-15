package io.token;

import io.token.proto.common.token.TokenProtos.Token;
import org.junit.Rule;
import org.junit.Test;

import static io.token.asserts.TokenAssertion.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

public class TransferTokenTest {
    @Rule public TokenRule rule = new TokenRule();
    private final Account payerAccount = rule.account();
    private final Account payeeAccount = rule.account();
    private final Member payer = payerAccount.member();
    private final Member payee = payeeAccount.member();

    @Test
    public void createTransferToken() {
        Token token = payer.createToken(
                100.0,
                "USD",
                payerAccount.id(),
                payee.firstAlias(),
                "book purchase");

        assertThat(token)
                .hasFrom(payer)
                .hasRedeemerAlias(payee.firstAlias())
                .hasAmount(100.0)
                .hasCurrency("USD")
                .hasNoSignatures();
    }

    @Test
    public void getTransferToken() {
        Token token = payer.createToken(100.0, "USD", payerAccount.id());
        assertThat(payer.getToken(token.getId()))
                .hasFrom(payer)
                .hasAmount(100.0)
                .hasCurrency("USD")
                .hasNoSignatures();
    }

    @Test
    public void getTransferTokens() {
        Token token1 = payer.createToken(123.45, "EUR", payerAccount.id());
        Token token2 = payer.createToken(678.90, "USD", payerAccount.id());
        Token token3 = payer.createToken(100.99, "USD", payerAccount.id());

        assertThat(payer.getTransferTokens(0, 100))
                .hasSize(3)
                .containsOnly(token1, token2, token3);
    }

    @Test
    public void endorseTransferToken() {
        Token token = payer.createToken(100.0, "USD", payerAccount.id());
        token = payer.endorseToken(token);

        assertThat(token)
                .hasNSignatures(2)
                .isEndorsedBy(payer)
                .hasFrom(payer)
                .hasAmount(100.0)
                .hasCurrency("USD");
    }

    @Test
    public void cancelTransferToken() {
        Token token = payer.createToken(100.0, "USD", payerAccount.id());
        token = payer.cancelToken(token);

        assertThat(token)
                .hasNSignatures(2)
                .isCancelledBy(payer)
                .hasFrom(payer)
                .hasAmount(100.0)
                .hasCurrency("USD");
    }
}
