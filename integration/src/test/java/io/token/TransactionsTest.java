package io.token;

import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import io.token.asserts.TransactionAssertion;
import io.token.common.LinkedAccount;
import io.token.common.TokenRule;
import io.token.proto.PagedList;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transfer.TransferProtos.Transfer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TransactionsTest {
    @Rule public TokenRule rule = new TokenRule();

    private LinkedAccount payerAccount;
    private Member payer;
    private LinkedAccount payeeAccount;
    private Member payee;

    @Before
    public void before() {
        this.payerAccount = rule.linkedAccount();
        this.payer = payerAccount.getMember();

        this.payeeAccount = rule.linkedAccount(payerAccount);
        this.payee = payeeAccount.getMember();
    }

    @Test
    public void getBalance() {
        assertThat(payerAccount.getBalance()).isGreaterThan(0);
    }

    @Test
    public void getTransaction() {
        Token token = token();
        token = payer.endorseToken(token, STANDARD).getToken();
        Transfer transfer = payee.redeemToken(
                token,
                100.0,
                payeeAccount.getCurrency(),
                null,
                null);

        Transaction transaction = payerAccount.getTransaction(transfer.getTransactionId());
        TransactionAssertion.assertThat(transaction)
                .isProcessing()
                .hasCurrency(payerAccount.getCurrency())
                .hasTokenId(token.getId())
                .hasTokenTransferId(transfer.getId());
    }

    @Test
    public void getTransaction_wrongId() {
        Transaction lookedUp = payerAccount.getAccount().getTransaction("invalid_id");
        assertThat(lookedUp).isNull();
    }

    @Test
    public void getTransaction_wrongAccount() {
        Token token = token();
        token = payer.endorseToken(token, STANDARD).getToken();
        Transfer transfer = payee.redeemToken(
                token,
                100.0,
                payeeAccount.getCurrency(),
                null,
                null);

        Transaction lookedUp =
                payeeAccount.getAccount().getTransaction(transfer.getTransactionId());
        assertThat(lookedUp).isNull();
    }

    @Test
    public void getTransactions() {
        List<Transfer> transfers = DoubleStream
                .of(100.0, 200.0, 300.0, 400.0, 500.0)
                .mapToObj(amount -> {
                    Token token = payerAccount
                            .createInstantToken(amount, payeeAccount)
                            .execute();
                    token = payer.endorseToken(token, STANDARD).getToken();
                    return payee.redeemToken(token);
                })
                .collect(toList());

        PagedList<Transaction, String> result = payerAccount.getTransactions(
                null,
                transfers.size() * 5); // To account for concurrent tests.

        List<Transaction> transactions = new ArrayList<>(result.getList());
        Assertions
                .assertThat(transactions
                        .stream()
                        .map(Transaction::getId)
                        .collect(toList()))
                .containsAll(transfers
                        .stream()
                        .map(Transfer::getTransactionId)
                        .collect(toList()));
    }

    @Test
    public void getTransactions_multiUse() {
        Token token = token();
        token = payer.endorseToken(token, STANDARD).getToken();

        final Transfer transfer1 = payee.redeemToken(
                token,
                100.0,
                payeeAccount.getCurrency(),
                "one");
        final Transfer transfer2 = payee.redeemToken(
                token,
                200.0,
                payeeAccount.getCurrency(),
                null,
                null);
        final Transfer transfer3 = payee.redeemToken(
                token,
                300.0,
                payeeAccount.getCurrency(),
                "three");
        final Transfer transfer4 = payee.redeemToken(
                token,
                400.0,
                payeeAccount.getCurrency(),
                "four",
                null);
        final Transfer transfer5 = payee.redeemToken(
                token,
                500.0,
                payeeAccount.getCurrency(),
                "five",
                null);

        PagedList<Transaction, String> result = payerAccount.getTransactions(null, 5);
        List<Transaction> transactions = new ArrayList<>(result.getList());
        transactions.sort(comparing(transaction -> transaction
                .getAmount()
                .getValue()));
        assertThat(result.getOffset()).isNotEmpty();

        double fee = Double.parseDouble(
                token.getPayload().getTransfer().getPricing().getSourceQuote().getFeesTotal());

        assertThat(transactions).hasSize(5);
        TransactionAssertion.assertThat(transactions.get(0))
                .isProcessing()
                .hasAmount(100.0 + fee)
                .hasTokenId(token.getId())
                .hasTokenTransferId(transfer1.getId());
        TransactionAssertion.assertThat(transactions.get(1))
                .isProcessing()
                .hasAmount(200.0 + fee)
                .hasTokenId(token.getId())
                .hasTokenTransferId(transfer2.getId());
        TransactionAssertion.assertThat(transactions.get(2))
                .isProcessing()
                .hasAmount(300.0 + fee)
                .hasTokenId(token.getId())
                .hasTokenTransferId(transfer3.getId())
                .containsDescription("three");
        TransactionAssertion.assertThat(transactions.get(3))
                .isProcessing()
                .hasAmount(400.0 + fee)
                .hasTokenId(token.getId())
                .hasTokenTransferId(transfer4.getId());
        TransactionAssertion.assertThat(transactions.get(4))
                .isProcessing()
                .hasAmount(500.0 + fee)
                .hasTokenId(token.getId())
                .hasTokenTransferId(transfer5.getId())
                .containsDescription("five");
    }

    @Test
    public void getTransactionsPaged() {
        int num = 10;
        IntStream
                .range(1, num + 1)
                .forEach(i -> {
                    Token token = payerAccount
                            .createInstantToken(i * 100, payeeAccount)
                            .execute();
                    token = payer.endorseToken(token, STANDARD).getToken();
                    payee.redeemToken(token);
                });

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
    public void getTransactionsPaged_multiUse() {
        Token token = token();
        token = payer.endorseToken(token, STANDARD).getToken();

        int num = 14;
        for (int i = 0; i < num; i++) {
            payee.redeemToken(
                    token,
                    100.0,
                    payeeAccount.getCurrency(),
                    null,
                    null);
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
        double initialBalance = payerAccount.getBalance();

        Token token = token();
        token = payer.endorseToken(token, STANDARD).getToken();
        payee.redeemToken(
                token,
                100.0,
                payeeAccount.getCurrency(),
                null,
                null);
        assertThat(initialBalance).isGreaterThan(payerAccount.getBalance());
    }

    private Token token() {
        return payerAccount.createInstantToken(1500.0, payeeAccount)
                .setRedeemerUsername(payee.firstUsername())
                .setDescription("Multi charge token")
                .execute();
    }
}
