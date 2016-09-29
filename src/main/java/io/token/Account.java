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
    public Member getMember() {
        return async.getMember().sync();
    }

    /**
     * @return account id
     */
    public String getId() {
        return async.getId();
    }

    /**
     * @return account name
     */
    public String getName() {
        return async.getName();
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
    public Money lookupBalance() {
        return async.lookupBalance().toBlocking().single();
    }

    /**
     * Looks up an existing transaction. Doesn't have to be a transaction for a token payment.
     *
     * @param transactionId ID of the transaction
     * @return transaction record
     */
    public Transaction lookupTransaction(String transactionId) {
        return async.lookupTransaction(transactionId).toBlocking().single();
    }

    /**
     * Looks up existing transactions. This is a full list of transactions with token payments
     * being a subset.
     *
     * @param offset offset to start at
     * @param limit max number of records to return
     * @return payment record
     */
    public List<Transaction> lookupTransactions(int offset, int limit) {
        return async.lookupTransactions(offset, limit).toBlocking().single();
    }
}
