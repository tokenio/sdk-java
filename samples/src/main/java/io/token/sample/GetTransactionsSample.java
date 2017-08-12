package io.token.sample;

import io.token.Account;
import io.token.Member;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transaction.TransactionProtos.TransactionStatus;
import io.token.proto.common.transaction.TransactionProtos.TransactionType;
import io.token.proto.common.transfer.TransferProtos.Transfer;

import java.util.List;

/**
 * Redeems a transfer token.
 */
public final class GetTransactionsSample {
    /**
     * Illustrate Member.getTransactions
     *
     * @param payer payer Token member
     */
    public static void getTransactionsSample(Member payer) {
        List<Account> accounts = payer.getAccounts();
        String accountId = accounts.get(0).id();

        for (Transaction transaction :
                payer.getTransactions(accountId, "0", 10).getList()) {
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
        List<Account> accounts = payer.getAccounts();
        String accountId = accounts.get(0).id();

        String transactionId = transfer.getTransactionId();
        Transaction transaction = payer.getTransaction(
                accountId,
                transactionId);
        return transaction;
    }

    /**
     * Illustrate Account.getTransactions
     *
     * @param payer payer Token member
     */
    public static void accountGetTransactionsSample(Member payer) {
        Account account = payer.getAccounts().get(0);

        for (Transaction transaction :
                account.getTransactions("0", 10).getList()) {
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
        Account account = payer.getAccounts().get(0);

        String txnId = transfer.getTransactionId();
        Transaction transaction = account.getTransaction(txnId);
        return transaction;
    }

    private static void displayTransaction(
            String currency,
            String value,
            TransactionType debitOrCredit,
            TransactionStatus status) {
    }
}
