package io.token;

import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.transaction.TransactionProtos.Transaction;

import java.util.List;

/**
 * Represents a funding account in the Token system.
 */
public final class Account {
    private final AccountAsync async;

    /**
     * @param async real implementation that the calls are delegated to
     */
    Account(AccountAsync async) {
        this.async = async;
    }

    /**
     * @return asynchronous version of the account API
     */
    public AccountAsync async() {
        return async;
    }

    /**
     * @return account owner
     */
    public Member member() {
        return async.member().sync();
    }

    /**
     * @return account id
     */
    public String id() {
        return async.id();
    }

    /**
     * @return account name
     */
    public String name() {
        return async.name();
    }

    /**
     * Sets a new bank account.
     *
     * @param newName new name to use
     */
    public void setAccountName(String newName) {
        async.setAccountName(newName).toBlocking().single();
    }


    /**
     * Looks up an account balance.
     *
     * @return account balance
     */
    public Money getBalance() {
        return async.getBalance().toBlocking().single();
    }

    /**
     * Looks up an existing transaction. Doesn't have to be a transaction for a token payment.
     *
     * @param transactionId ID of the transaction
     * @return transaction record
     */
    public Transaction getTransaction(String transactionId) {
        return async.getTransaction(transactionId).toBlocking().single();
    }

    /**
     * Looks up existing transactions by using an access token
     *
     * @param offset offset to start at
     * @param limit max number of records to return
     * @return payment record
     */
    public List<Transaction> getTransactions(int offset, int limit) {
        return async.getTransactions(offset, limit).toBlocking().single();
    }
}
