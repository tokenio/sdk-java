package io.token.sample;

import io.token.Member;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transfer.TransferProtos.Transfer;

/**
 * Redeems a payment token.
 */
public final class RedeemPaymentTokenSample {
    /**
     * Redeems a payment token to transfer money from payer bank account to payee bank account.
     *
     * @param payee payee Token member
     * @param tokenId ID of the token to redeem
     * @return a payment Transfer
     */
    public static Transfer redeemToken(Member payee, String tokenId) {
        // Retrieve a payment token to redeem.
        Token token = payee.getToken(tokenId);

        // Payee redeems a payment token. Money is transferred to a payee bank account.
        Transfer transfer = payee.redeemToken(token);

        return transfer;
    }
}
