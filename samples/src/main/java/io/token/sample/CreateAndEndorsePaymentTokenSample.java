package io.token.sample;

import io.token.Member;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos.Token;

/**
 * Creates a payment token and endorses it to a payee.
 */
public final class CreateAndEndorsePaymentTokenSample {
    /**
     * Creates a payment token and authorizes a money transfer from a payer to a payee.
     *
     * @param payer payer Token member
     * @param payeeUsername payee Token member username
     * @return a payment Token
     */
    public static Token createToken(Member payer, String payeeUsername) {
        // Create a payment token.
        Token token = payer.createToken(
                100.0,                              /* amount */
                "EUR",                              /* currency */
                payer.getAccounts().get(0).id(),    /* payer account to transfer money from */
                payeeUsername,                      /* payee token username to transfer money to */
                "Book purchase"                     /* optional payment description */
        );

        // Payer endorses a token to a payee by signing it with its secure private key.
        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();

        return token;
    }
}
