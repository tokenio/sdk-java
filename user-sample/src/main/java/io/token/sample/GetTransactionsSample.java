package io.token.sample;

import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;

import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transaction.TransactionProtos.TransactionStatus;
import io.token.proto.common.transaction.TransactionProtos.TransactionType;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.user.Account;
import io.token.user.Member;

import java.util.List;

public final class GetTransactionsSample {
    /**
     * Illustrate Member.getTransactions
     *
     * @param payer payer Token member
     */
    public static void getTransactionsSample(Member payer) {
        List<Account> accounts = payer.getAccountsBlocking();
        String accountId = accounts.get(0).id();
        for (Transaction transaction :
                payer.getTransactionsBlocking(accountId, null, 10, STANDARD).getList()) {
            displayTransaction(
                    transaction.getAmount().getCurrency(),
                    transaction.getAmount().getValue(),
                    transaction.getType(), // debit or credit
                    transaction.getStatus());
        }
    }

    /**
     * Illustrate Member.getTransactions
     *
     * @param payer payer Token member
     */
    public static void getTransactionsByDateSample(Member payer) {
        List<Account> accounts = payer.getAccountsBlocking();
        String accountId = accounts.get(0).id();
        for (Transaction transaction :
                payer.getTransactionsBlocking(
                        accountId,
                        null,
                        10,
                        STANDARD,
                        "2019-01-15",
                        "2022-02-15")
                        .getList()) {
            displayTransaction(
                    transaction.getAmount().getCurrency(),
                    transaction.getAmount().getValue(),
                    transaction.getType(), // debit or credit
                    transaction.getStatus());
        }
    }

    /**
     * Illustrate Member.getTransaction
     *
     * @param payer payer Token member
     * @param transfer recently-completed transfer
     * @return a Transaction
     */
    public static Transaction getTransactionSample(
            Member payer,
            Transfer transfer) {
        List<Account> accounts = payer.getAccountsBlocking();
        String accountId = accounts.get(0).id();

        String transactionId = transfer.getTransactionId();
        Transaction transaction = payer.getTransactionBlocking(accountId, transactionId, STANDARD);
        return transaction;
    }

    /**
     * Illustrate Account.getTransactions
     *
     * @param payer payer Token member
     */
    public static void accountGetTransactionsSample(Member payer) {
        Account account = payer.getAccountsBlocking().get(0);

        for (Transaction transaction : account.getTransactionsBlocking(null, 10, STANDARD)
                .getList()) {
            displayTransaction(
                    transaction.getAmount().getCurrency(),
                    transaction.getAmount().getValue(),
                    transaction.getType(), // debit or credit
                    transaction.getStatus());
        }
    }

    /**
     * Illustrate Account.getTransaction
     *
     * @param payer payer Token member
     * @param transfer recently-completed transfer
     * @return a Transaction
     */
    public static Transaction accountGetTransactionSample(
            Member payer,
            Transfer transfer) {
        Account account = payer.getAccountsBlocking().get(0);

        String txnId = transfer.getTransactionId();
        Transaction transaction = account.getTransactionBlocking(txnId, STANDARD);
        return transaction;
    }

    private static void displayTransaction(
            String currency,
            String value,
            TransactionType debitOrCredit,
            TransactionStatus status) {
    }
}
