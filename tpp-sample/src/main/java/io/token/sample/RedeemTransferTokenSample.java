package io.token.sample;

import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferEndpoint;
import io.token.tpp.Member;
import io.token.util.Util;

/**
 * Redeems a transfer token.
 */
public final class RedeemTransferTokenSample {
    /**
     * Redeems a transfer token to transfer money from payer bank account to payee bank account.
     *
     * @param payee payee Token member
     * @param accountId account id of the payee
     * @param tokenId ID of the token to redeem
     * @return a transfer Transfer
     */
    public static Transfer redeemTransferToken(
            Member payee,
            String accountId, // account ID of the payee
            String tokenId) { // ID of token to redeem
        // We'll use this as a reference ID. Normally, a payee who
        // explicitly sets a reference ID would use an ID from a db.
        // E.g., an online merchant might use the ID of a "shopping cart".
        // We don't have a db, so we fake it with a random string:
        String cartId = Util.generateNonce();

        // Retrieve a transfer token to redeem.
        Token transferToken = payee.getTokenBlocking(tokenId);

        // Set token destination
        TransferEndpoint tokenDestination = TransferEndpoint
                .newBuilder()
                .setAccount(BankAccount.newBuilder()
                        .setToken(BankAccount.Token.newBuilder()
                                .setMemberId(payee.memberId())
                                .setAccountId(accountId)))
                .build();

        // Payee redeems a transfer token.
        // Money is transferred to a payee bank account.
        Transfer transfer = payee.redeemTokenBlocking(
                transferToken,
                tokenDestination,
                // if refId not set, transfer will have random refID:
                cartId);

        return transfer;
    }
}
