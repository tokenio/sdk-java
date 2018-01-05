/**
 * Copyright (c) 2017 Token, Inc.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.token;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.token.proto.PagedList;
import io.token.proto.common.account.AccountProtos;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.gateway.Gateway.GetBalanceResponse;
import io.token.proto.gateway.Gateway.GetTransactionResponse;
import io.token.proto.gateway.Gateway.GetTransactionsResponse;
import io.token.rpc.Client;

import javax.annotation.Nullable;

/**
 * Represents a funding account in the Token system.
 */
public class AccountAsync {
    private final MemberAsync member;
    private final AccountProtos.Account.Builder account;
    private final Client client;

    /**
     * Creates an instance.
     *
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
     * Returns a sync version of the account API.
     *
     * @return synchronous version of the account API
     */
    public Account sync() {
        return new Account(this);
    }

    /**
     * Gets an account owner.
     *
     * @return account owner
     */
    public MemberAsync member() {
        return member;
    }

    /**
     * Gets an account ID.
     *
     * @return account id
     */
    public String id() {
        return account.getId();
    }

    /**
     * Sets this account as a member's default account.
     *
     * @return completable
     */
    public Completable setAsDefault() {
        return client.setDefaultAccount(this.id());
    }

    /**
     * Looks up if this account is default.
     *
     * @return true if this account is default; false otherwise.
     */
    public Observable<Boolean> isDefault() {
        return client.isDefault(this.id());
    }

    /**
     * Gets an account name.
     *
     * @return account name
     */
    public String name() {
        return account.getName();
    }

    /**
     * Looks up if this account is locked.
     *
     * @return true if this account is locked; false otherwise.
     */
    public boolean isLocked() {
        return account.getIsLocked();
    }

    /**
     * Gets a bank ID.
     *
     * @return bank ID
     */
    public String bankId() {
        return account.getBankId();
    }

    /**
     * Looks up an account available balance.
     *
     * @param keyLevel key level
     * @return account available balance
     */
    public Observable<Money> getAvailableBalance(Key.Level keyLevel) {
        return client.getBalance(account.getId(), keyLevel)
                .map(new Function<GetBalanceResponse, Money>() {
                    public Money apply(GetBalanceResponse response) {
                        return response.getAvailable();
                    }
                });
    }

    /**
     * Looks up an account current balance.
     *
     * @param keyLevel key level
     * @return account current balance
     */
    public Observable<Money> getCurrentBalance(Key.Level keyLevel) {
        return client.getBalance(account.getId(), keyLevel)
                .map(new Function<GetBalanceResponse, Money>() {
                    public Money apply(GetBalanceResponse response) {
                        return response.getCurrent();
                    }
                });
    }

    /**
     * Looks up an account current balance.
     *
     * @param keyLevel key level
     * @return account current balance
     */
    public Observable<GetBalanceResponse> getBalance(Key.Level keyLevel) {
        return client.getBalance(account.getId(), keyLevel);
    }

    /**
     * Looks up an existing transaction. Doesn't have to be a transaction for a token transfer.
     *
     * @param transactionId ID of the transaction
     * @param keyLevel key level
     * @return transaction record
     */
    public Observable<Transaction> getTransaction(String transactionId, Key.Level keyLevel) {
        return client.getTransaction(account.getId(), transactionId, keyLevel)
                .map(new Function<GetTransactionResponse, Transaction>() {
                    public Transaction apply(GetTransactionResponse response) {
                        return response.getTransaction();
                    }
                });
    }

    /**
     * Lookup transaction.
     *
     * @param transactionId transaction id
     * @param keyLevel key level
     * @return transaction response
     */
    public Observable<GetTransactionResponse> getTransactionResponse(
            String transactionId,
            Key.Level keyLevel) {
        return client.getTransaction(account.getId(), transactionId, keyLevel);
    }

    /**
     * Looks up existing transactions. This is a full list of transactions with token transfers
     * being a subset.
     *
     * @param offset offset to start at
     * @param limit max number of records to return
     * @param keyLevel key level
     * @return list of transactions
     */
    public Observable<PagedList<Transaction, String>> getTransactions(
            @Nullable String offset,
            int limit,
            Key.Level keyLevel) {
        return client.getTransactions(account.getId(), offset, limit, keyLevel)
                .map(new Function<GetTransactionsResponse, PagedList<Transaction, String>>() {
                    public PagedList<Transaction, String> apply(GetTransactionsResponse response) {
                        return PagedList.create(
                                response.getTransactionsList(),
                                response.getOffset());
                    }
                });
    }

    /**
     * Lookup transactions.
     *
     * @param offset offset
     * @param limit limit
     * @param keyLevel key level
     * @return transactions response
     */
    public Observable<GetTransactionsResponse> getTransactionsResponse(
            @Nullable String offset,
            int limit,
            Key.Level keyLevel) {
        return client.getTransactions(account.getId(), offset, limit, keyLevel);
    }

    @Override
    public int hashCode() {
        return account.getId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AccountAsync)) {
            return false;
        }

        AccountAsync other = (AccountAsync) obj;
        return account.build().equals(other.account.build());
    }
}
