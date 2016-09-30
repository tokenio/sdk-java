package io.token;

import io.token.proto.common.account.AccountProtos;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.rpc.Client;
import rx.Observable;

import java.util.List;

/**
 * Represents a funding account in the Token system.
 */
public final class AccountAsync {
    private final MemberAsync member;
    private final AccountProtos.Account.Builder account;
    private final Client client;

    /**
     * @param member account owner
     * @param account account information
     * @param client RPC client used to perform operations against the server
     */
    AccountAsync(MemberAsync member, AccountProtos.Account account, Client client) {
        this.member = member;
        this.account = account.toBuilder();
        this.client = client;
    }

    /**
     * @return synchronous version of the account API
     */
    public Account sync() {
        return new Account(this);
    }

    /**
     * @return account owner
     */
    public MemberAsync getMember() {
        return member;
    }

    /**
     * @return account id
     */
    public String getId() {
        return account.getId();
    }

    /**
     * @return account name
     */
    public String getName() {
        return account.getName();
    }

    /**
     * Sets a new bank account.
     *
     * @param newName new name to use
     */
    public Observable<Void> setAccountName(String newName) {
        return client
                .setAccountName(account.getId(), newName)
                .map(a -> {
                    this.account.clear().mergeFrom(a);
                    return null;
                });
    }

    /**
     * Looks up an account balance.
     *
     * @return account balance
     */
    public Observable<Money> lookupBalance() {
        return client.lookupBalance(account.getId());
    }

    /**
     * Looks up an existing transaction. Doesn't have to be a transaction for a token payment.
     *
     * @param transactionId ID of the transaction
     * @return transaction record
     */
    public Observable<Transaction> lookupTransaction(String transactionId) {
        return client.lookupTransaction(account.getId(), transactionId);
    }

    /**
     * Looks up existing transactions. This is a full list of transactions with token payments
     * being a subset.
     *
     * @param offset offset to start at
     * @param limit max number of records to return
     * @return payment record
     */
    public Observable<List<Transaction>> lookupTransactions(int offset, int limit) {
        return client.lookupTransactions(account.getId(), offset, limit);
    }
}
