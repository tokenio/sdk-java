/**
 * Copyright (c) 2018 Token, Inc.
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
import io.token.proto.common.member.MemberProtos.AddressRecord;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.transaction.TransactionProtos.Balance;
import io.token.proto.common.transaction.TransactionProtos.Transaction;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Acts as another member by using a granted access token.
 */
public class DelegateMember {
    private final Member member;

    public DelegateMember(Member member, String tokenId) {
        this.member = member;
        this.member.useAccessToken(tokenId);
    }

    /**
     * Looks up member addresses.
     *
     * @return a list of addresses
     */
    public List<AddressRecord> getAddresses() {
        return member.getAddresses();
    }

    /**
     * Looks up an address by id.
     *
     * @param addressId the address id
     * @return an address record
     */
    public AddressRecord getAddress(String addressId) {
        return member.getAddress(addressId);
    }

    /**
     * Looks up funding bank accounts linked to Token.
     *
     * @return list of linked accounts
     */
    public List<Account> getAccounts() {
        return member.getAccounts();
    }

    /**
     * Looks up a funding bank account linked to Token.
     *
     * @param accountId account id
     * @return looked up account
     */
    public Account getAccount(String accountId) {
        return member.getAccount(accountId);
    }

    /**
     * Looks up account balance.
     *
     * @param accountId account id
     * @param keyLevel key level
     * @return balance
     */
    public Balance getBalance(String accountId, Key.Level keyLevel) {
        return member.getBalance(accountId, keyLevel);
    }

    /**
     * Looks up transactions for a given account.
     *
     * @param accountId the account id
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @param keyLevel key level
     * @return paged list of transactions
     */
    public PagedList<Transaction, String> getTransactions(
            String accountId,
            @Nullable String offset,
            int limit,
            Key.Level keyLevel) {
        return member.getTransactions(accountId, offset, limit, keyLevel);
    }

    /**
     * Looks up an existing transaction for a given account.
     *
     * @param accountId the account id
     * @param transactionId ID of the transaction
     * @param keyLevel key level
     * @return transaction
     */
    public Transaction getTransaction(
            String accountId,
            String transactionId,
            Key.Level keyLevel) {
        return member.getTransaction(accountId, transactionId, keyLevel);
    }
}
