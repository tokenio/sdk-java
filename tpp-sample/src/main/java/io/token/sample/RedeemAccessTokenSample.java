package io.token.sample;

import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;

import io.token.proto.PagedList;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.transaction.TransactionProtos.StandingOrder;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.tpp.Account;
import io.token.tpp.Member;
import io.token.tpp.Representable;

import java.util.List;

/**
 * Redeems an information access token.
 */
public final class RedeemAccessTokenSample {
    /**
     * Redeems access token to acquire access to the grantor's account balances.
     *
     * @param grantee grantee Token member
     * @param tokenId ID of the access token to redeem
     * @return balance of one of grantor's accounts
     */
    public static Money redeemBalanceAccessToken(Member grantee, String tokenId) {
        // Specifies whether the request originated from a customer
        boolean customerInitiated = true;

        // Access grantor's account list by applying
        // access token to the grantee client.
        // forAccessToken snippet begin
        Representable grantor = grantee.forAccessToken(tokenId, customerInitiated);
        List<Account> accounts = grantor.getAccountsBlocking();

        // Get the data we want
        Money balance0 = accounts.get(0).getBalanceBlocking(STANDARD).getCurrent();
        // forAccessToken snippet end
        return balance0;
    }

    /**
     * Redeems access token to acquire access to the grantor's account transactions.
     *
     * @param grantee grantee Token member
     * @param tokenId ID of the access token to redeem
     * @return transaction history of one of grantor's accounts
     */
    public static List<Transaction> redeemTransactionsAccessToken(Member grantee, String tokenId) {
        // Specifies whether the request originated from a customer
        boolean customerInitiated = true;

        // Access grantor's account list by applying
        // access token to the grantee client.
        // forAccessToken snippet begin
        Representable grantor = grantee.forAccessToken(tokenId, customerInitiated);
        List<Account> accounts = grantor.getAccountsBlocking();

        // Get the first 10 transactions
        PagedList<Transaction, String> transactions = accounts.get(0)
                .getTransactionsBlocking(null, 10, STANDARD);

        // Pass this offset to the next getTransactions
        // call to fetch the next page of transactions.
        String nextOffset = transactions.getOffset();

        return transactions.getList();
    }

    /**
     * Redeems access token to acquire access to the grantor's standing orders at an account.
     *
     * @param grantee grantee Token member
     * @param tokenId ID of the access token to redeem
     * @return standing orders of one of grantor's accounts
     */
    public static List<StandingOrder> redeemStandingOrdersAccessToken(
            Member grantee,
            String tokenId) {
        // Specifies whether the request originated from a customer
        boolean customerInitiated = true;

        // Access grantor's account list by applying
        // access token to the grantee client.
        // forAccessToken snippet begin
        Representable grantor = grantee.forAccessToken(tokenId, customerInitiated);
        List<Account> accounts = grantor.getAccountsBlocking();

        // Get the first 5 standing orders
        PagedList<StandingOrder, String> standingOrders = accounts.get(0)
                .getStandingOrdersBlocking(null, 5, STANDARD);

        // Pass this offset to the next getStandingOrders
        // call to fetch the next page of transactions.
        String nextOffset = standingOrders.getOffset();

        return standingOrders.getList();
    }
}
