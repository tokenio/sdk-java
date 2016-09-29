package io.token;

import io.token.proto.common.payment.PaymentProtos.Payment;
import io.token.proto.common.token.TokenProtos.Token;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static io.token.asserts.PaymentAssertion.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

public class PaymentRedemptionTest {
    @Rule public TokenRule rule = new TokenRule();
    private final Account payerAccount = rule.account();
    private final Account payeeAccount = rule.account();
    private final Member payer = payerAccount.getMember();
    private final Member payee = payeeAccount.getMember();

    @Test
    public void redeemToken() {
        Token token = payer.createToken(
                100.0,
                "USD",
                payerAccount.getId(),
                payee.getFirstAlias(),
                "book purchase");
        token = payer.endorseToken(token);

        Payment payment = payee.redeemToken(token);
        assertThat(payment)
                .hasAmount(100.0)
                .hasCurrency("USD")
                .hasNSignatures(2)
                .isSignedBy(payee);
    }

    @Test
    public void redeemToken_withParams() {
        Token token = payer.createToken(
                100.0,
                "USD",
                payerAccount.getId(),
                payee.getFirstAlias(),
                "book purchase");
        token = payer.endorseToken(token);

        Payment payment = payee.redeemToken(token, 99.0, "USD");
        assertThat(payment)
                .hasAmount(99.0)
                .hasCurrency("USD")
                .hasNSignatures(2)
                .isSignedBy(payee);
    }

    @Test
    public void lookupPayment() {
        Token token = payer.createToken(
                100.0,
                "USD",
                payerAccount.getId(),
                payee.getFirstAlias(),
                "book purchase");
        token = payer.endorseToken(token);

        Payment payment = payee.redeemToken(token);
        Payment lookedUp = payer.lookupPayment(payment.getId());
        assertThat(lookedUp).isEqualTo(payment);
    }

    @Test
    public void lookupPayments() {
        Token token = payer.createToken(
                100.0,
                "USD",
                payerAccount.getId(),
                payee.getFirstAlias(),
                "book purchase");
        token = payer.endorseToken(token);

        Payment payment1 = payee.redeemToken(token, 10.0, "USD");
        Payment payment2 = payee.redeemToken(token, 20.0, "USD");
        Payment payment3 = payee.redeemToken(token, 70.0, "USD");

        assertThat(payment1)
                .hasAmount(10.0)
                .hasCurrency("USD");
        assertThat(payment2)
                .hasAmount(20.0)
                .hasCurrency("USD");
        assertThat(payment3)
                .hasAmount(70.0)
                .hasCurrency("USD");

        List<Payment> lookedUp = payer.lookupPayments(0, 100, token.getId());
        assertThat(lookedUp).containsOnly(payment1, payment2, payment3);
    }
}
