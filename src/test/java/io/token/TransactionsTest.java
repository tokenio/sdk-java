package io.token;

import io.token.asserts.TransactionAssertion;
import io.token.proto.common.payment.PaymentProtos.Payment;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static java.lang.Double.parseDouble;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class TransactionsTest {
    @Rule public TokenRule rule = new TokenRule();
    private final Account payerAccount = rule.account();
    private final Account payeeAccount = rule.account();
    private final Member payer = payerAccount.member();
    private final Member payee = payeeAccount.member();

    @Test
    public void getBalance() {
        assertThat(parseDouble(payerAccount.getBalance().getValue())).isGreaterThan(0);
        assertThat(payerAccount.getBalance().getCurrency()).isEqualTo("USD");
    }

    @Test
    public void getTransaction() {
        Token token = payer.createPaymentToken(
                1000.0,
                "USD",
                payerAccount.id(),
                payee.firstAlias(),
                "Multi charge token");
        token = payer.endorsePaymentToken(token);
        Payment payment = payee.redeemPaymentToken(token, 100.0, "USD");

        Transaction transaction = payerAccount.getTransaction(payment.getReferenceId());
        TransactionAssertion.assertThat(transaction)
                .hasAmount(100.0)
                .hasCurrency("USD")
                .hasTokenId(token.getId())
                .hasTokenPaymentId(payment.getId());
    }

    @Test
    public void getTransactions() {
        Token token = payer.createPaymentToken(
                1000.0,
                "USD",
                payerAccount.id(),
                payee.firstAlias(),
                "Multi charge token");
        token = payer.endorsePaymentToken(token);

        Payment payment1 = payee.redeemPaymentToken(token, 100.0, "USD");
        Payment payment2 = payee.redeemPaymentToken(token, 200.0, "USD");
        Payment payment3 = payee.redeemPaymentToken(token, 300.0, "USD");

        List<Transaction> transactions = payerAccount.getTransactions(0, 3).stream()
                .sorted((t1, t2) -> t1.getAmount().getValue().compareTo(t2.getAmount().getValue()))
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
