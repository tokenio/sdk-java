package io.token.sample;

import io.token.Destination;
import io.token.Member;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transfer.TransferProtos.Transfer;

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
    public static Transfer redeemTransferToken(Member payee, String accountId, String tokenId) {
        // Retrieve a transfer token to redeem.
        Token transferToken = payee.getToken(tokenId);

        // Payee redeems a transfer token. Money is transferred to a payee bank account.
        Transfer transfer = payee.redeemToken(
                transferToken,
                Destination.token(accountId, payee.memberId()));

        return transfer;
    }
}
