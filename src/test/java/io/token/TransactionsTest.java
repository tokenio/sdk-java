package io.token;

import io.token.asserts.TransactionAssertion;
import io.token.proto.common.payment.PaymentProtos.Payment;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class TransactionsTest {
    @Rule public TokenRule rule = new TokenRule();
    private final Account payer = rule.account();
    private final Account payee = rule.account();

    @Test
    public void lookupBalance() {
        assertThat(payer.lookupBalance().getValue()).isGreaterThan(0);
        assertThat(payer.lookupBalance().getCurrency()).isEqualTo("USD");
    }

    @Test
    public void lookupTransaction() {
        Token token = payer.createToken(
                1000.0,
                "USD",
                payee.getMember().getFirstAlias(),
                "Multi charge token");
        token = payer.endorseToken(token);
        Payment payment = payee.redeemToken(token, 100.0, "USD");

        List<Transaction> transactions = payee.lookupTransactions(0, 10);
        assertThat(transactions).isNotEmpty();
        TransactionAssertion.assertThat(transactions.get(0))
                .hasAmount(100.0)
                .hasCurrency("USD")
                .hasTokenId(token.getId())
                .hasTokenPaymentId(payment.getId());

        Transaction transaction = payee.lookupTransaction(transactions.get(0).getId());
        TransactionAssertion.assertThat(transaction).isEqualTo(transactions.get(0));
    }

    @Test
    public void lookupTransactions() {
        Token token = payer.createToken(
                1000.0,
                "USD",
                payee.getMember().getFirstAlias(),
                "Multi charge token");
        token = payer.endorseToken(token);

        Payment payment1 = payee.redeemToken(token, 100.0, "USD");
        Payment payment2 = payee.redeemToken(token, 200.0, "USD");
        Payment payment3 = payee.redeemToken(token, 300.0, "USD");

        List<Transaction> transactions = payee.lookupTransactions(0, 3).stream()
                .sorted((t1, t2) -> Double.compare(t1.getAmount().getValue(), t2.getAmount().getValue()))
                .collect(toList());

        assertThat(transactions).hasSize(3);
        TransactionAssertion.assertThat(transactions.get(0))
                .hasAmount(100.0)
                .hasCurrency("USD")
                .hasTokenId(token.getId())
                .hasTokenPaymentId(payment1.getId());
        TransactionAssertion.assertThat(transactions.get(1))
                .hasAmount(200.0)
                .hasCurrency("USD")
                .hasTokenId(token.getId())
                .hasTokenPaymentId(payment2.getId());
        TransactionAssertion.assertThat(transactions.get(2))
                .hasAmount(300.0)
                .hasCurrency("USD")
                .hasTokenId(token.getId())
                .hasTokenPaymentId(payment3.getId());
    }
}
