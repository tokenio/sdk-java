package io.token.asserts;

import io.token.proto.common.transaction.TransactionProtos.Transaction;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public final class TransactionAssertion extends AbstractAssert<TransactionAssertion, Transaction> {
    public static TransactionAssertion assertThat(Transaction transaction) {
        return new TransactionAssertion(transaction);
    }

    private TransactionAssertion(Transaction actual) {
        super(actual, TransactionAssertion.class);
    }

    public TransactionAssertion hasAmount(double amount) {
        Assertions.assertThat(actual.getAmount().getValue()).isEqualTo(Double.toString(amount));
        return this;
    }

    public TransactionAssertion hasCurrency(String currency) {
        Assertions.assertThat(actual.getAmount().getCurrency()).isEqualTo(currency);
        return this;
    }

    public TransactionAssertion hasTokenId(String tokenId) {
        Assertions.assertThat(actual.getTokenId()).isEqualTo(tokenId);
        return this;
    }

    public TransactionAssertion hasTokenPaymentId(String tokenPaymentId) {
        Assertions.assertThat(actual.getTokenPaymentId()).isEqualTo(tokenPaymentId);
        return this;
    }
}

