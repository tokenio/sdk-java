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

package io.token.user;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.token.Member;
import io.token.proto.common.account.AccountProtos;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.transaction.TransactionProtos.Balance;
import io.token.user.rpc.Client;

/**
 * Represents a funding account in the Token system.
 */
public class Account extends io.token.Account {
    private final Client client;

    Account(Member member, AccountProtos.Account account, Client client) {
        super(member, account, client);
        this.client = client;
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
     * Sets this account as a member's default account.
     */
    public void setAsDefaultBlocking() {
        setAsDefault().blockingAwait();
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
     * Looks up if this account is default.
     *
     * @return true if this account is default; false otherwise.
     */
    public boolean isDefaultBlocking() {
        return isDefault().blockingSingle();
    }

    /**
     * Looks up an account current balance.
     *
     * @param keyLevel key level
     * @return account current balance
     */
    public Observable<Money> getCurrentBalance(Key.Level keyLevel) {
        return client.getBalance(account.getId(), keyLevel).map(new Function<Balance, Money>() {
            @Override
            public Money apply(Balance balance) throws Exception {
                return balance.getCurrent();
            }
        });
    }

    /**
     * Looks up an account current balance.
     *
     * @param keyLevel key level
     * @return account current balance
     */
    public Money getCurrentBalanceBlocking(Key.Level keyLevel) {
        return getCurrentBalance(keyLevel).blockingSingle();
    }

    /**
     * Looks up an account available balance.
     *
     * @param keyLevel key level
     * @return account available balance
     */
    public Observable<Money> getAvailableBalance(Key.Level keyLevel) {
        return client.getBalance(account.getId(), keyLevel).map(new Function<Balance, Money>() {
            @Override
            public Money apply(Balance balance) throws Exception {
                return balance.getAvailable();
            }
        });
    }

    /**
     * Looks up an account available balance.
     *
     * @param keyLevel key level
     * @return account available balance
     */
    public Money getAvailableBalanceBlocking(Key.Level keyLevel) {
        return getAvailableBalance(keyLevel).blockingSingle();
    }
}
