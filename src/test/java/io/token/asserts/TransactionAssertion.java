package io.token.asserts;

import io.token.proto.common.transaction.TransactionProtos.Transaction;

import java.math.BigDecimal;
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
        Assertions
                .assertThat(normalize(new BigDecimal(actual.getAmount().getValue())))
                .isEqualTo(normalize(BigDecimal.valueOf(amount)));
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

    public TransactionAssertion hasTokenTransferId(String tokenTransferId) {
        Assertions.assertThat(actual.getTokenTransferId()).isEqualTo(tokenTransferId);
        return this;
    }

    public TransactionAssertion containsDescription(String description) {
        Assertions.assertThat(actual.getDescription()).contains(description);
        return this;
    }

    private static BigDecimal normalize(BigDecimal decimal) {
        return decimal.setScale(2, BigDecimal.ROUND_UNNECESSARY);
    }
}

