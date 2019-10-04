/**
 * Copyright (c) 2019 Token, Inc.
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

import io.reactivex.Observable;
import io.token.proto.PagedList;
import io.token.proto.common.account.AccountProtos;
import io.token.proto.common.account.AccountProtos.AccountDetails;
import io.token.proto.common.account.AccountProtos.AccountFeatures;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.transaction.TransactionProtos.Balance;
import io.token.proto.common.transaction.TransactionProtos.StandingOrder;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.rpc.Client;

import javax.annotation.Nullable;

/**
 * Represents a funding account in the Token system.
 */
public class Account {
    protected final Member member;
    protected final AccountProtos.Account account;
    protected final Client client;

    /**
     * Copy constructor. For internal use.
     *
     * @param account account
     */
    public Account(Account account) {
        this.member = account.member;
        this.account = account.account;
        this.client = account.client;
    }

    protected Account(Member member, AccountProtos.Account account, Client client) {
        this.member = member;
        this.account = account;
        this.client = client;
    }

    /**
     * Gets an account owner.
     *
     * @return account owner
     */
    public Member member() {
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
     * Gets the account details.
     *
     * @return account details
     */
    public AccountDetails accountDetails() {
        return account.getAccountDetails();
    }

    /**
     * Gets the account features.
     *
     * @return account features
     */
    public AccountFeatures accountFeatures() {
        return account.getAccountFeatures();
    }

    /**
     * Fetches the original {@link AccountProtos.Account} object.
     *
     * @return the account.
     */
    public AccountProtos.Account toProto() {
        return account;
    }

    /**
     * Looks up an account balance.
     *
     * @param keyLevel key level
     * @return account balance
     */
    public Observable<Balance> getBalance(Key.Level keyLevel) {
        return client.getBalance(account.getId(), keyLevel);
    }

    /**
     * Looks up an account balance.
     *
     * @param keyLevel key level
     * @return account balance
     */
    public Balance getBalanceBlocking(Key.Level keyLevel) {
        return getBalance(keyLevel).blockingSingle();
    }

    /**
     * Looks up a transaction by ID.
     *
     * @param transactionId transaction ID
     * @param keyLevel key level
     * @return transaction
     */
    public Observable<Transaction> getTransaction(String transactionId, Key.Level keyLevel) {
        return client.getTransaction(account.getId(), transactionId, keyLevel);
    }

    /**
     * Looks up a transaction by ID.
     *
     * @param transactionId transaction ID
     * @param keyLevel key level
     * @return transaction
     */
    public Transaction getTransactionBlocking(String transactionId, Key.Level keyLevel) {
        return getTransaction(transactionId, keyLevel).blockingSingle();
    }

    /**
     * Looks up transactions.
     *
     * @param offset offset
     * @param limit limit
     * @param keyLevel key level
     * @return paged list of transactions
     */
    public Observable<PagedList<Transaction, String>> getTransactions(
            @Nullable String offset,
            int limit,
            Key.Level keyLevel) {
        return getTransactions(offset, limit, keyLevel, null, null);
    }

    /**
     * Looks up transactions with date filter.
     *
     * @param offset offset
     * @param limit limit
     * @param keyLevel key level
     * @param startDate inclusive lower bound of transaction booking date
     * @param endDate inclusive upper bound of transaction booking date
     * @return paged list of transactions
     */
    public Observable<PagedList<Transaction, String>> getTransactions(
            @Nullable String offset,
            int limit,
            Key.Level keyLevel,
            @Nullable String startDate,
            @Nullable String endDate) {
        return client.getTransactions(account.getId(), offset, limit, keyLevel, startDate, endDate);
    }

    /**
     * Looks up transactions.
     *
     * @param offset offset
     * @param limit limit
     * @param keyLevel key level
     * @return paged list of transactions
     */
    public PagedList<Transaction, String> getTransactionsBlocking(
            @Nullable String offset,
            int limit,
            Key.Level keyLevel) {
        return getTransactions(offset, limit, keyLevel).blockingSingle();
    }

    /**
     * Looks up transactions.
     *
     * @param offset offset
     * @param limit limit
     * @param keyLevel key level
     * @param startDate inclusive lower bound of transaction booking date
     * @param endDate inclusive upper bound of transaction booking date
     * @return paged list of transactions
     */
    public PagedList<Transaction, String> getTransactionsBlocking(
            @Nullable String offset,
            int limit,
            Key.Level keyLevel,
            @Nullable String startDate,
            @Nullable String endDate) {
        return getTransactions(offset, limit, keyLevel, startDate, endDate).blockingSingle();
    }

    /**
     * Looks up an existing standing order for a given account.
     *
     * @param standingOrderId ID of the standing order
     * @param keyLevel key level
     * @return standing order record
     */
    public Observable<StandingOrder> getStandingOrder(String standingOrderId, Key.Level keyLevel) {
        return client.getStandingOrder(account.getId(), standingOrderId, keyLevel);
    }

    /**
     * Looks up an existing standing order for a given account.
     *
     * @param standingOrderId ID of the standing order
     * @param keyLevel key level
     * @return standing order record
     */
    public StandingOrder getStandingOrderBlocking(
            String standingOrderId,
            Key.Level keyLevel) {
        return getStandingOrder(standingOrderId, keyLevel).blockingSingle();
    }

    /**
     * Looks up standing orders for a given account.
     *
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @param keyLevel key level
     * @return a paged list of standing order records
     */
    public Observable<PagedList<StandingOrder, String>> getStandingOrders(
            @Nullable String offset,
            int limit,
            Key.Level keyLevel) {
        return client.getStandingOrders(account.getId(), offset, limit, keyLevel);
    }

    /**
     * Looks up standing orders for a given account.
     *
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @param keyLevel key level
     * @return a paged list of standing order records
     */
    public PagedList<StandingOrder, String> getStandingOrdersBlocking(
            @Nullable String offset,
            int limit,
            Key.Level keyLevel) {
        return getStandingOrders(offset, limit, keyLevel).blockingSingle();
    }

    @Override
    public int hashCode() {
        return account.getId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Account)) {
            return false;
        }

        Account other = (Account) obj;
        return account.equals(other.account);
    }
}
