package io.token.sample;

import io.token.Account;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TransferBody;
import io.token.proto.common.transaction.TransactionProtos.TransactionStatus;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.tpp.Member;

import java.util.List;

public final class GetTransfersSample {
    /**
     * Illustrate Member.getTransfers
     *
     * @param payer payer Token member
     */
    public static void getTransfersSample(Member payer) {
        List<Account> accounts = payer.getAccountsBlocking();
        String accountId = accounts.get(0).id();

        for (Transfer transfer :
                payer.getTransfersBlocking(null, 10, null).getList()) {
            displayTransfer(
                    transfer.getStatus(),
                    transfer.getPayload().getDescription());
        }
    }

    /**
     * Illustrate Member.getTransferTokens
     *
     * @param payer payer Token member
     */
    public static void getTransferTokensSample(
            Member payer) {
        for (Token token : payer.getTransferTokensBlocking(null, 10)
                .getList()) {
            TransferBody transferBody = token.getPayload().getTransfer();
            displayTransferToken(
                    transferBody.getCurrency(),
                    transferBody.getLifetimeAmount());
        }
    }


    /**
     * Illustrate Member.getTransfer
     *
     * @param payer payer Token member
     * @param transferId id of a transfer
     * @return a Transfer
     */
    public static Transfer getTransferSample(
            Member payer,
            String transferId) {
        Transfer transfer = payer.getTransferBlocking(transferId);
        return transfer;
    }

    private static void displayTransfer(
            TransactionStatus status,
            String description) {
    }

    private static void displayTransferToken(
            String currency,
            String value) {
    }
}
