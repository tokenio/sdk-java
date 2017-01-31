package io.token;

import io.token.proto.PagedList;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.transaction.TransactionProtos.Transaction;

import javax.annotation.Nullable;

/**
 * Represents a funding account in the Token system.
 */
public final class Account {
    private final AccountAsync async;

    /**
     * Creates an instance.
     *
     * @param async real implementation that the calls are delegated to
     */
    Account(AccountAsync async) {
        this.async = async;
    }

    /**
     * Returns an async version of the API.
     *
     * @return asynchronous version of the account API
     */
    public AccountAsync async() {
        return async;
    }

    /**
     * Gets a sync version of the Member API.
     *
     * @return account owner
     */
    public Member member() {
        return async.member().sync();
    }

    /**
     * Gets an acount id.
     *
     * @return account id
     */
    public String id() {
        return async.id();
    }

    /**
     * Gets an account name.
     *
     * @return account name
     */
    public String name() {
        return async.name();
    }

    /**
     * Gets a bank ID.
     *
     * @return bank ID
     */
    public String bankId() {
        return async.bankId();
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
     * Looks up an existing transaction. Doesn't have to be a transaction for a token transfer.
     *
     * @param transactionId ID of the transaction
     * @return transaction record
     */
    public Transaction getTransaction(String transactionId) {
        return async.getTransaction(transactionId).toBlocking().single();
    }

    /**
     * Looks up existing transactions by using an access token.
     *
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @return list of transactions
     */
    public PagedList<Transaction, String> getTransactions(@Nullable String offset, int limit) {
        return async.getTransactions(offset, limit).toBlocking().single();
    }
}
