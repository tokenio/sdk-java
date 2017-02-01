package io.token.sample;

import io.token.Member;
import io.token.proto.common.token.TokenProtos;

/**
 * Creates a payment token.
 */
public final class CreatePaymentTokenSample {
    /**
     * Creates a payment token to transfer money from a payer to a payee.
     *
     * @param payer payer Token member
     * @param payeeUsername payee Token member username
     * @return a payment Token
     */
    public static TokenProtos.Token createToken(Member payer, String payeeUsername) {
        // Create a payment token.
        TokenProtos.Token paymentToken = payer.createToken(
                100.0,                              /* amount */
                "EUR",                              /* currency */
                payer.getAccounts().get(0).id(),    /* payer account to transfer money from */
                payeeUsername,                      /* payee token username to transfer money to */
                "Book purchase"                     /* optional payment description */
        );

        return paymentToken;
    }
}
