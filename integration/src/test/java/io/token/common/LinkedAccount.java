package io.token.common;

import static org.assertj.core.api.Assertions.assertThat;

import io.token.Account;
import io.token.Member;
import io.token.TransferTokenBuilder;
import io.token.bank.TestAccount;
import io.token.proto.PagedList;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos;

import javax.annotation.Nullable;

/**
 * {@link Account} object exposed by the SDK along with its currency.
 */
public class LinkedAccount {
    private final TestAccount testAccount;
    private final Account account;

    public LinkedAccount(TestAccount testAccount, Account account) {
        this.testAccount = testAccount;
        this.account = account;
    }

    public String getId() {
        return account.id();
    }

    public Account getAccount() {
        return account;
    }

    public Member getMember() {
        return getAccount().member();
    }

    public TransferTokenBuilder createTransferToken(double amount) {
        return getMember()
                .createTransferToken(amount, getCurrency())
                .setAccountId(getId());
    }

    public TransferTokenBuilder createTransferToken(double amount, LinkedAccount destination) {
        return getMember()
                .createTransferToken(amount, destination.getCurrency())
                .setAccountId(getId())
                .addDestination(TransferInstructionsProtos.TransferEndpoint.newBuilder()
                        .setAccount(BankAccount.newBuilder()
                                .setToken(BankAccount.Token.newBuilder()
                                        .setMemberId(destination.getMember().memberId())
                                        .setAccountId(destination.getId())))
                        .build());
    }

    public Double getBalance() {
        Money balance = account.getBalance();
        assertThat(balance.getCurrency()).isEqualTo(getCurrency());
        return Double.parseDouble(balance.getValue());
    }

    public Transaction getTransaction(String transactionId) {
        Transaction transaction = account.getTransaction(transactionId);
        assertThat(transaction.getId()).isEqualTo(transactionId);
        assertThat(transaction.getAmount().getCurrency()).isEqualTo(getCurrency());
        return transaction;
    }

    public PagedList<Transaction, String> getTransactions(@Nullable String offset, int limit) {
        PagedList<Transaction, String> transactions = account.getTransactions(offset, limit);
        transactions.getList().forEach(
                t -> assertThat(t.getAmount().getCurrency()).isEqualTo(getCurrency()));
        return transactions;
    }

    public String getCurrency() {
        return testAccount.getCurrency();
    }

    public TestAccount testAccount() {
        return testAccount;
    }
}
