package io.token.sample;

import io.token.Member;
import io.token.proto.common.security.SecurityProtos;
import io.token.proto.common.token.TokenProtos;
import io.token.proto.common.transfer.TransferProtos;

/**
 * Redeems a payment token.
 */
public final class RedeemPaymentTokenSample {
    /**
     * Redeems a payment token to transfer money from payer bank account to payee bank account.
     *
     * @param payer payer Token member
     * @param payee payee Token member
     * @return a payment Transfer
     */
    public static TransferProtos.Transfer redeemToken(Member payer, Member payee) {
        // Create a payment token using CreatePaymentTokenSample.
        TokenProtos.Token token =
                CreatePaymentTokenSample.createToken(payer, payee.firstUsername());

        // Payer endorses token to a payee by signing it with its secure private key.
        token = payer.endorseToken(token, SecurityProtos.Key.Level.STANDARD).getToken();

        // Payee redeems a payment token. Money is transferred to a payee bank account.
        TransferProtos.Transfer transfer = payee.redeemToken(token);

        return transfer;
    }
}
