package io.token;

import io.token.proto.common.token.TokenProtos.PaymentToken;
import org.junit.Rule;
import org.junit.Test;

import static io.token.asserts.TokenAssertion.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

public class PaymentTokenTest {
    @Rule public TokenRule rule = new TokenRule();
    private final Account payerAccount = rule.account();
    private final Account payeeAccount = rule.account();
    private final Member payer = payerAccount.member();
    private final Member payee = payeeAccount.member();

    @Test
    public void createPaymentToken() {
        PaymentToken token = payer.createPaymentToken(
                100.0,
                "USD",
                payerAccount.id(),
                payee.firstAlias(),
                "book purchase");

        assertThat(token)
                .hasPayer(payer)
                .hasRedeemerAlias(payee.firstAlias())
                .hasAmount(100.0)
                .hasCurrency("USD")
                .hasNoSignatures();
    }

    @Test
    public void getPaymentToken() {
        PaymentToken token = payer.createPaymentToken(100.0, "USD", payerAccount.id());
        assertThat(payer.getPaymentToken(token.getId()))
                .hasPayer(payer)
                .hasAmount(100.0)
                .hasCurrency("USD")
                .hasNoSignatures();
    }

    @Test
    public void getPaymentTokens() {
        PaymentToken token1 = payer.createPaymentToken(123.45, "EUR", payerAccount.id());
        PaymentToken token2 = payer.createPaymentToken(678.90, "USD", payerAccount.id());
        PaymentToken token3 = payer.createPaymentToken(100.99, "USD", payerAccount.id());

        assertThat(payer.getPaymentTokens(0, 100))
                .hasSize(3)
                .containsOnly(token1, token2, token3);
    }

    @Test
    public void endorsePaymentToken() {
        PaymentToken token = payer.createPaymentToken(100.0, "USD", payerAccount.id());
        token = payer.endorsePaymentToken(token);

        assertThat(token)
                .hasNSignatures(2)
                .isEndorsedBy(payer)
                .hasPayer(payer)
                .hasAmount(100.0)
                .hasCurrency("USD");
    }

    @Test
    public void cancelPaymentToken() {
        PaymentToken token = payer.createPaymentToken(100.0, "USD", payerAccount.id());
        token = payer.cancelPaymentToken(token);

        assertThat(token)
                .hasNSignatures(2)
                .isCancelledBy(payer)
                .hasPayer(payer)
                .hasAmount(100.0)
                .hasCurrency("USD");
    }
}
