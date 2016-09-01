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
    private final Account account = rule.account();

    @Test
    public void redeemToken() {
        Token token = account.createToken(100.0, "USD", "amazon", "book purchase");
        token = account.endorseToken(token);

        Payment payment = account.redeemToken(token);
        assertThat(payment)
                .hasAmount(100.0)
                .hasCurrency("USD")
                .hasNSignatures(2)
                .isSignedBy(account.getMember());
    }

    @Test
    public void redeemToken_withParams() {
        Token token = account.createToken(100.0, "USD", "amazon", "book purchase");
        token = account.endorseToken(token);

        Payment payment = account.redeemToken(token, 99.0, "USD");
        assertThat(payment)
                .hasAmount(99.0)
                .hasCurrency("USD")
                .hasNSignatures(2)
                .isSignedBy(account.getMember());
    }

    @Test
    public void lookupPayment() {
        Token token = account.createToken(100.0, "USD", "amazon", "book purchase");
        token = account.endorseToken(token);

        Payment payment = account.redeemToken(token);
        Payment lookedUp = account.lookupPayment(payment.getId());
        assertThat(lookedUp).isEqualTo(payment);
    }

    @Test
    public void lookupPayments() {
        Token token = account.createToken(100.0, "USD", "amazon", "book purchase");
        token = account.endorseToken(token);

        Payment payment1 = account.redeemToken(token, 10.0, "USD");
        Payment payment2 = account.redeemToken(token, 20.0, "USD");
        Payment payment3 = account.redeemToken(token, 70.0, "USD");

        assertThat(payment1)
                .hasAmount(10.0)
                .hasCurrency("USD");
        assertThat(payment2)
                .hasAmount(20.0)
                .hasCurrency("USD");
        assertThat(payment3)
                .hasAmount(70.0)
                .hasCurrency("USD");

        List<Payment> lookedUp = account.lookupPayments(0, 100, token.getId());
        assertThat(lookedUp).containsOnly(payment1, payment2, payment3);
    }
}
