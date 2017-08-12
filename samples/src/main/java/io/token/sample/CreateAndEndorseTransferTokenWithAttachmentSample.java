package io.token.sample;

import static io.token.proto.common.blob.BlobProtos.Blob.AccessMode.DEFAULT;

import io.token.Member;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.blob.BlobProtos.Attachment;
import io.token.proto.common.blob.BlobProtos.Blob;
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
     *
     * @param payer Payer member token
     * @param payeeAlias Token member alias of payee
     * @return Token
     */
    public static Token createTransferTokenWithNewAttachment(
            Member payer,
            Alias payeeAlias) {

        // Create a transfer token.
        Token transferToken =
                payer.createTransferToken(100.0, "EUR")
                        .setAccountId(payer.getAccounts().get(0).id())
                        .setRedeemerAlias(payeeAlias)
                        .setDescription("Invoice payment")
                        .addAttachment(
                                payer.memberId(),
                                "image/jpeg",
                                "invoice.jpg",
                                loadImageByteArray("invoice.jpg"))
                        .execute();

        transferToken = payer.endorseToken(
                transferToken,
                Key.Level.STANDARD).getToken();

        return transferToken;
    }

    /**
     * Create a new transfer token, including an attached image file.
     *
     * @param payer Payer member token
     * @param payeeId Token member username of payee
     * @return Token
     */
    public static Token createTransferTokenWithExistingAttachment(
            Member payer,
            String payeeId) {

        Attachment attachment = payer.createBlob(
                payeeId,
                "image/jpeg",
                "invoice.jpg",
                loadImageByteArray("invoice.jpg"),
                DEFAULT);

        // Create a transfer token.
        Token transferToken =
                payer.createTransferToken(100.0, "EUR")
                        .setAccountId(payer.getAccounts().get(0).id())
                        .setRedeemerMemberId(payeeId)
                        .setDescription("Invoice payment")
                        .addAttachment(attachment)
                        .execute();

        // Payer endorses a token to a payee by signing it with her secure private key.
        transferToken = payer.endorseToken(
                transferToken,
                Key.Level.STANDARD).getToken();

        Blob copyBlob = payer.getBlob(attachment.getBlobId());

        return transferToken;
    }
}
