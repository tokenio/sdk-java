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
    private final Member payer = payerAccount.getMember();
    private final Member payee = payeeAccount.getMember();

    @Test
    public void createPaymentToken() {
        PaymentToken token = payer.createToken(
                100.0,
                "USD",
                payerAccount.getId(),
                payee.getFirstAlias(),
                "book purchase");

        assertThat(token)
                .hasPayer(payer)
                .hasRedeemerAlias(payee.getFirstAlias())
                .hasAmount(100.0)
                .hasCurrency("USD")
                .hasNoSignatures();
    }

    @Test
    public void lookupPaymentToken() {
        PaymentToken token = payer.createToken(100.0, "USD", payerAccount.getId());
        assertThat(payer.lookupToken(token.getId()))
                .hasPayer(payer)
                .hasAmount(100.0)
                .hasCurrency("USD")
                .hasNoSignatures();
    }

    @Test
    public void lookupPaymentTokens() {
        PaymentToken token1 = payer.createToken(123.45, "EUR", payerAccount.getId());
        PaymentToken token2 = payer.createToken(678.90, "USD", payerAccount.getId());
        PaymentToken token3 = payer.createToken(100.99, "USD", payerAccount.getId());

        assertThat(payer.lookupTokens(0, 100))
                .hasSize(3)
                .containsOnly(token1, token2, token3);
    }

    @Test
    public void endorsePaymentToken() {
        PaymentToken token = payer.createToken(100.0, "USD", payerAccount.getId());
        token = payer.endorseToken(token);

        assertThat(token)
                .hasNSignatures(2)
                .isEndorsedBy(payer)
                .hasPayer(payer)
                .hasAmount(100.0)
                .hasCurrency("USD");
    }

    @Test
    public void declinePaymentToken() {
        PaymentToken token = payer.createToken(100.0, "USD", payerAccount.getId());
        token = payer.declineToken(token);

        assertThat(token)
                .hasNSignatures(2)
                .isDeclinedBy(payer)
                .hasPayer(payer)
                .hasAmount(100.0)
                .hasCurrency("USD");
    }

    @Test
    public void revokePaymentToken() {
        PaymentToken token = payer.createToken(100.0, "USD", payerAccount.getId());
        token = payer.endorseToken(token);
        token = payer.revokeToken(token);

        assertThat(token)
                .hasNSignatures(4)
                .isEndorsedBy(payer)
                .isRevokedBy(payer)
                .hasPayer(payer)
                .hasAmount(100.0)
                .hasCurrency("USD");
    }
}
