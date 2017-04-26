package io.token;

import static java.lang.Double.parseDouble;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import io.token.asserts.TransactionAssertion;
import io.token.proto.PagedList;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.Destination;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.Destination.SwiftDestination;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.Destination.TokenDestination;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;

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
        Token token = token();
        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();
        Transfer transfer = payee.redeemToken(token, 100.0, "USD", null, null);

        Transaction transaction = payerAccount.getTransaction(transfer.getReferenceId());
        TransactionAssertion.assertThat(transaction)
                .isSuccessful()
                .hasAmount(100.0)
                .hasCurrency("USD")
                .hasTokenId(token.getId())
                .hasTokenTransferId(transfer.getId());
    }

    @Test
    public void getTransactions() {
        Destination destination = Destination.newBuilder()
                .setSwiftDestination(SwiftDestination.getDefaultInstance())
                .build();
        Token token = token(destination);
        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();

        final Transfer transfer1 = payee.redeemToken(token, 100.0, "USD", "one");
        final Transfer transfer2 = payee.redeemToken(token, 200.0, "USD", null, null);
        final Transfer transfer3 = payee.redeemToken(token, 300.0, "USD", "three");
        final Transfer transfer4 = payee.redeemToken(token, 400.0, "USD", destination);
        final Transfer transfer5 = payee.redeemToken(token, 500.0, "USD", "five", destination);

        PagedList<Transaction, String> result = payerAccount.getTransactions(null, 5);
        List<Transaction> transactions = new ArrayList<>(result.getList());
        Collections.sort(transactions, new Comparator<Transaction>() {
            public int compare(Transaction thisTransaction, Transaction otherTransaction) {
                return thisTransaction.getAmount().getValue()
                        .compareTo(otherTransaction.getAmount().getValue());
            }
        });
        assertThat(result.getOffset()).isNotEmpty();

        assertThat(transactions).hasSize(5);
        TransactionAssertion.assertThat(transactions.get(0))
                .isSuccessful()
                .hasAmount(100.0)
                .hasCurrency("USD")
                .hasTokenId(token.getId())
                .hasTokenTransferId(transfer1.getId());
        TransactionAssertion.assertThat(transactions.get(1))
                .isSuccessful()
                .hasAmount(200.0)
                .hasCurrency("USD")
                .hasTokenId(token.getId())
                .hasTokenTransferId(transfer2.getId());
        TransactionAssertion.assertThat(transactions.get(2))
                .isSuccessful()
                .hasAmount(300.0)
                .hasCurrency("USD")
                .hasTokenId(token.getId())
                .hasTokenTransferId(transfer3.getId())
                .containsDescription("three");
        TransactionAssertion.assertThat(transactions.get(3))
                .isSuccessful()
                .hasAmount(400.0)
                .hasCurrency("USD")
                .hasTokenId(token.getId())
                .hasTokenTransferId(transfer4.getId());
        TransactionAssertion.assertThat(transactions.get(4))
                .isSuccessful()
                .hasAmount(500.0)
                .hasCurrency("USD")
                .hasTokenId(token.getId())
                .hasTokenTransferId(transfer5.getId())
                .containsDescription("five");
    }

    @Test
    public void getTransactionsPaged() {
        Token token = token();
        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();

        int num = 14;
        for (int i = 0; i < num; i++) {
            payee.redeemToken(token, 100.0, "USD", null, null);
        }

        int limit = 2;
        ImmutableSet.Builder<Transaction> builder = ImmutableSet.builder();
        PagedList<Transaction, String> result = payerAccount.getTransactions(null, limit);
        for (int i = 0; i < num / limit; i++) {
            builder.addAll(result.getList());
            result = payerAccount.getTransactions(result.getOffset(), limit);
        }

        assertThat(builder.build().size()).isEqualTo(num);
    }

    @Test
    public void testBalanceUpdate() {
        double initialBalance = parseDouble(payerAccount.getBalance().getValue());
        Token token = token();
        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();
        payee.redeemToken(token, 100.0, "USD", null, null);
        double finalBalance = parseDouble(payerAccount.getBalance().getValue());
        assertThat(initialBalance).isGreaterThan(finalBalance);
    }

    private Token token() {
        return token(Destination.newBuilder()
                .setTokenDestination(TokenDestination.newBuilder()
                        .setAccountId(payeeAccount.id())
                        .setMemberId(payee.memberId())
                        .build())
                .build());
    }

    private Token token(Destination destination) {
        return payer.createToken(
                1500.0,
                "USD",
                payerAccount.id(),
                payee.firstUsername(),
                "Multi charge token",
                destination);
    }
}
