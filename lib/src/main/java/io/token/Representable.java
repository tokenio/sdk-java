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

import static io.token.proto.common.member.MemberProtos.AddressRecord;
import static io.token.proto.common.security.SecurityProtos.Key;
import static io.token.proto.common.transaction.TransactionProtos.Balance;
import static io.token.proto.common.transaction.TransactionProtos.Transaction;

import io.token.proto.PagedList;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferEndpoint;

import java.util.List;
import javax.annotation.Nullable;

/**
 * **DEPRECATED** Use api.Representable instead.
 *
 * <p>Represents the part of a token member that can be accessed through an access token.
 */
@Deprecated
public interface Representable {
    /**
     * Looks up member addresses.
     *
     * @return a list of addresses
     */
    List<AddressRecord> getAddresses();

    /**
     * Looks up an address by id.
     *
     * @param addressId the address id
     * @return an address record
     */
    AddressRecord getAddress(String addressId);

    /**
     * Looks up funding bank accounts linked to Token.
     *
     * @return list of linked accounts
     */
    List<Account> getAccounts();

    /**
     * Looks up a funding bank account linked to Token.
     *
     * @param accountId account id
     * @return looked up account
     */
    Account getAccount(String accountId);

    /**
     * Looks up account balance.
     *
     * @param accountId account id
     * @param keyLevel key level
     * @return balance
     */
    Balance getBalance(String accountId, Key.Level keyLevel);

    /**
     * Looks up balances for a list of accounts.
     *
     * @param accountIds list of account ids
     * @param keyLevel key level
     * @return list of balances
     */
    List<Balance> getBalances(List<String> accountIds, Key.Level keyLevel);

    /**
     * Looks up an existing transaction for a given account.
     *
     * @param accountId the account id
     * @param transactionId ID of the transaction
     * @param keyLevel key level
     * @return transaction
     */
    Transaction getTransaction(
            String accountId,
            String transactionId,
            Key.Level keyLevel);

    /**
     * Looks up transactions for a given account.
     *
     * @param accountId the account id
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @param keyLevel key level
     * @return paged list of transactions
     */
    PagedList<Transaction, String> getTransactions(
            String accountId,
            @Nullable String offset,
            int limit,
            Key.Level keyLevel);

    /**
     * Resolves transfer destinations for the given account ID.
     *
     * @param accountId account ID
     * @return transfer endpoints
     */
    List<TransferEndpoint> resolveTransferDestinations(String accountId);
}
