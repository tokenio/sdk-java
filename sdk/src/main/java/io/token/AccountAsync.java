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

import io.token.proto.PagedList;
import io.token.proto.common.account.AccountProtos;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.rpc.Client;

import javax.annotation.Nullable;

import rx.Observable;

/**
 * Represents a funding account in the Token system.
 */
public final class AccountAsync {
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
     * Gets an account name.
     *
     * @return account name
     */
    public String name() {
        return account.getName();
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
     * Looks up an account balance.
     *
     * @return account balance
     */
    public Observable<Money> getBalance() {
        return client.getBalance(account.getId());
    }

    /**
     * Looks up an existing transaction. Doesn't have to be a transaction for a token transfer.
     *
     * @param transactionId ID of the transaction
     * @return transaction record
     */
    public Observable<Transaction> getTransaction(String transactionId) {
        return client.getTransaction(account.getId(), transactionId);
    }

    /**
     * Looks up existing transactions. This is a full list of transactions with token transfers
     * being a subset.
     *
     * @param offset offset to start at
     * @param limit max number of records to return
     * @return list of transactions
     */
    public Observable<PagedList<Transaction, String>> getTransactions(
            @Nullable String offset,
            int limit) {
        return client.getTransactions(account.getId(), offset, limit);
    }
}
