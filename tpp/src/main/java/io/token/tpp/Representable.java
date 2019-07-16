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

package io.token.tpp;

import static io.token.proto.common.security.SecurityProtos.Key;
import static io.token.proto.common.transaction.TransactionProtos.Balance;
import static io.token.proto.common.transaction.TransactionProtos.Transaction;

import io.reactivex.Observable;
import io.token.proto.PagedList;
import io.token.proto.common.transaction.TransactionProtos;
import io.token.proto.common.transaction.TransactionProtos.StandingOrder;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferDestination;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Represents the part of a token member that can be accessed through an access token.
 */
public interface Representable {
    /**
     * Links a funding bank account to Token and returns it to the caller.
     *
     * @return list of accounts
     */
    Observable<List<Account>> getAccounts();

    /**
     * Looks up funding bank accounts linked to Token.
     *
     * @return list of linked accounts
     */
    List<Account> getAccountsBlocking();

    /**
     * Looks up a funding bank account linked to Token.
     *
     * @param accountId account id
     * @return looked up account
     */
    Observable<Account> getAccount(String accountId);

    /**
     * Looks up a funding bank account linked to Token.
     *
     * @param accountId account id
     * @return looked up account
     */
    Account getAccountBlocking(String accountId);

    /**
     * Looks up account balance.
     *
     * @param accountId the account id
     * @param keyLevel key level
     * @return balance
     */
    Observable<Balance> getBalance(String accountId, Key.Level keyLevel);

    /**
     * Looks up account balance.
     *
     * @param accountId account id
     * @param keyLevel key level
     * @return balance
     */
    Balance getBalanceBlocking(String accountId, Key.Level keyLevel);

    /**
     * Looks up balances for a list of accounts.
     *
     * @param accountIds list of account ids
     * @param keyLevel key level
     * @return list of balances
     */
    Observable<List<Balance>> getBalances(List<String> accountIds, Key.Level keyLevel);

    /**
     * Looks up balances for a list of accounts.
     *
     * @param accountIds list of account ids
     * @param keyLevel key level
     * @return list of balances
     */
    List<Balance> getBalancesBlocking(List<String> accountIds, Key.Level keyLevel);

    /**
     * Looks up an existing transaction for a given account.
     *
     * @param accountId the account id
     * @param transactionId ID of the transaction
     * @param keyLevel key level
     * @return transaction record
     */
    Observable<Transaction> getTransaction(
            String accountId,
            String transactionId,
            Key.Level keyLevel);

    /**
     * Looks up an existing transaction for a given account.
     *
     * @param accountId the account id
     * @param transactionId ID of the transaction
     * @param keyLevel key level
     * @return transaction
     */
    Transaction getTransactionBlocking(
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
     * @return a paged list of transaction records
     */
    Observable<PagedList<Transaction, String>> getTransactions(
            String accountId,
            @Nullable String offset,
            int limit,
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
    PagedList<Transaction, String> getTransactionsBlocking(
            String accountId,
            @Nullable String offset,
            int limit,
            Key.Level keyLevel);

    /**
     * Looks up an existing standing order for a given account.
     *
     * @param accountId the account ID
     * @param standingOrderId ID of the standing order
     * @param keyLevel key level
     * @return standing order record
     */
    Observable<StandingOrder> getStandingOrder(
            String accountId,
            String standingOrderId,
            Key.Level keyLevel);

    /**
     * Looks up an existing standing order for a given account.
     *
     * @param accountId the account ID
     * @param standingOrderId ID of the standing order
     * @param keyLevel key level
     * @return standing order record
     */
    StandingOrder getStandingOrderBlocking(
            String accountId,
            String standingOrderId,
            Key.Level keyLevel);

    /**
     * Looks up standing orders for a given account.
     *
     * @param accountId the account ID
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @param keyLevel key level
     * @return a paged list of standing order records
     */
    Observable<PagedList<StandingOrder, String>> getStandingOrders(
            String accountId,
            @Nullable String offset,
            int limit,
            Key.Level keyLevel);

    /**
     * Looks up standing orders for a given account.
     *
     * @param accountId the account ID
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @param keyLevel key level
     * @return a paged list of standing order records
     */
    PagedList<StandingOrder, String> getStandingOrdersBlocking(
            String accountId,
            @Nullable String offset,
            int limit,
            Key.Level keyLevel);

    /**
     * Resolves transfer destinations for the given account ID.
     *
     * @param accountId account ID
     * @return transfer destinations
     */
    Observable<List<TransferDestination>> resolveTransferDestinations(String accountId);

    /**
     * Resolves transfer destinations for the given account ID.
     *
     * @param accountId account ID
     * @return transfer destinations
     */
    List<TransferDestination> resolveTransferDestinationsBlocking(String accountId);

    /**
     * Confirm that the given account has sufficient funds to cover the charge.
     *
     * @param accountId account ID
     * @param amount charge amount
     * @param currency charge currency
     * @return true if the account has sufficient funds to cover the charge
     */
    Observable<Boolean> confirmFunds(String accountId, double amount, String currency);

    /**
     * Confirm that the given account has sufficient funds to cover the charge.
     *
     * @param accountId account ID
     * @param amount charge amount
     * @param currency charge currency
     * @return true if the account has sufficient funds to cover the charge
     */
    boolean confirmFundsBlocking(String accountId, double amount, String currency);
}
