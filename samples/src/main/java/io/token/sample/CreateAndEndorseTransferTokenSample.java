package io.token.sample;

import io.token.Member;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos.Token;

/**
 * Creates a transfer token and endorses it to a payee.
 */
public final class CreateAndEndorseTransferTokenSample {
    /**
     * Creates a transfer token and authorizes a money transfer from a payer to a payee.
     *
     * @param payer payer Token member
     * @param payeeUsername payee Token member username
     * @return a transfer Token
     */
    public static Token createTransferToken(Member payer, String payeeUsername) {
        // Create a transfer token.
        Token transferToken = payer.createToken(
                100.0,                              /* amount */
                "EUR",                              /* currency */
                payer.getAccounts().get(0).id(),    /* payer account to transfer money from */
                payeeUsername,                      /* payee token username to transfer money to */
                "Book purchase"                     /* optional transfer description */
        );

        // Payer endorses a token to a payee by signing it with her secure private key.
        transferToken = payer.endorseToken(transferToken, Key.Level.STANDARD).getToken();

        return transferToken;
    }
}
