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
     * @param payeeAlias payee Token member alias
     * @return a transfer Token
     */
    public static Token createTransferToken(
            Member payer,
            String payeeAlias) {

        // Create a transfer token.
        Token transferToken =
                payer.createTransferToken(
                        100.0, // amount
                        "EUR")  // currency
                        .setAccountId(payer.getAccounts().get(0).id()) // source account
                        .setRedeemerAlias(payeeAlias) // payee token alias
                        .setDescription("Book purchase") // optional description
                        .execute();

        // Payer endorses a token to a payee by signing it with her secure private key.
        transferToken = payer.endorseToken(
                transferToken,
                Key.Level.STANDARD).getToken();

        return transferToken;
    }
}
