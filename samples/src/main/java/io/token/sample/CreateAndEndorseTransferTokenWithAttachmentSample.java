package io.token.sample;

import io.token.Member;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos.Token;

/**
 * Creates a transfer token, attaches a file, and endorses it to a payee.
 */
public final class CreateAndEndorseTransferTokenWithAttachmentSample {
    // Fake function to make example more plausible
    private static byte[] loadImageByteArray(String filename) {
        return new byte[0];
    }

    /**
     * Create a new transfer token, including an attached image file.
     * @param payer Payer member token
     * @param payeeAlias Token member alias of payee
     * @return Token
     */
    public static Token createTransferTokenWithNewAttachment(
            Member payer,
            Alias payeeAlias) {

        // Create a transfer token.
        Token transferToken =
                payer.createTransferToken(
                        100.0, // amount
                        "EUR")  // currency
                        .setAccountId(payer.getAccounts().get(0).id()) // source account
                        .setRedeemerAlias(payeeAlias) // payee token alias
                        .setDescription("Invoice payment") // optional description
                        .addAttachment(
                                payer.memberId(),
                                "image/jpeg",
                                "invoice.jpg",
                                loadImageByteArray("invoice.jpg"))
                        .execute();

        // Payer endorses a token to a payee by signing it with her secure private key.
        transferToken = payer.endorseToken(
                transferToken,
                Key.Level.STANDARD).getToken();

        return transferToken;
    }
}
