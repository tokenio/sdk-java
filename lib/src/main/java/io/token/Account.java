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
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.transaction.TransactionProtos.Balance;
import io.token.proto.common.transaction.TransactionProtos.Transaction;

import javax.annotation.Nullable;

/**
 * **DEPRECATED** Use api.Account instead.
 *
 * <p>Represents a funding account in the Token system.
 */
@Deprecated
public class Account {
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
     * Gets an account id.
     *
     * @return account id
     */
    public String id() {
        return async.id();
    }

    /**
     * Sets to be a default account for its member.
     * Only 1 account can be default for each member.
     */
    public void setAsDefault() {
        async.setAsDefault().blockingAwait();
    }

    /**
     * Checks if this account is default.
     *
     * @return true if the account is default; otherwise false
     */
    public boolean isDefault() {
        return async.isDefault().blockingSingle();
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
     * Looks up if this account is locked.
     *
     * @return true if this account is locked; false otherwise.
     */
    public boolean isLocked() {
        return async.isLocked();
    }

    /**
     * Fetches the original {@link AccountProtos.Account} object.
     *
     * @return the account.
     */
    public AccountProtos.Account toProto() {
        return async.toProto();
    }

    /**
     * Looks up an account balance.
     *
     * @param keyLevel key level
     * @return account balance
     */
    public Balance getBalance(Key.Level keyLevel) {
        return async.getBalance(keyLevel).blockingSingle();
    }

    /**
     * Looks up an account current balance.
     *
     * @param keyLevel key level
     * @return account current balance
     */
    public Money getCurrentBalance(Key.Level keyLevel) {
        return async.getCurrentBalance(keyLevel).blockingSingle();
    }

    /**
     * Looks up an account available balance.
     *
     * @param keyLevel key level
     * @return account available balance
     */
    public Money getAvailableBalance(Key.Level keyLevel) {
        return async.getAvailableBalance(keyLevel).blockingSingle();
    }

    /**
     * Lookup transaction.
     *
     * @param transactionId transaction id
     * @param keyLevel key level
     * @return transaction
     */
    public Transaction getTransaction(
            String transactionId,
            Key.Level keyLevel) {
        return async.getTransaction(transactionId, keyLevel).blockingSingle();
    }

    /**
     * Lookup transactions.
     *
     * @param offset offset
     * @param limit limit
     * @param keyLevel key level
     * @return paged list of transactions
     */
    public PagedList<Transaction, String> getTransactions(
            @Nullable String offset,
            int limit,
            Key.Level keyLevel) {
        return async.getTransactions(offset, limit, keyLevel).blockingSingle();
    }

    @Override
    public int hashCode() {
        return async.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Account) {
            return async.equals(((Account) obj).async);
        } else {
            return async.equals(obj);
        }
    }
}
