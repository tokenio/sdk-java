package io.token.sample;

import static io.token.proto.common.blob.BlobProtos.Blob.AccessMode.DEFAULT;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.blob.BlobProtos.Attachment;
import io.token.proto.common.blob.BlobProtos.Blob;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.user.Member;

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
                        .setAccountId(payer.getAccountsBlocking().get(0).id())
                        .setToAlias(payeeAlias)
                        .setDescription("Invoice payment")
                        .addAttachment(
                                payer.memberId(),
                                "image/jpeg",
                                "invoice.jpg",
                                loadImageByteArray("invoice.jpg"))
                        .execute();

        transferToken = payer.endorseTokenBlocking(
                transferToken,
                Key.Level.STANDARD).getToken();

        return transferToken;
    }

    /**
     * Create a new transfer token, including an attached image file.
     *
     * @param payer Payer member token
     * @param payeeId Token member id of payee
     * @return Token
     */
    public static Token createTransferTokenWithExistingAttachment(
            Member payer,
            String payeeId) {

        Attachment attachment = payer.createBlobBlocking(
                payeeId,
                "image/jpeg",
                "invoice.jpg",
                loadImageByteArray("invoice.jpg"),
                DEFAULT);

        // Create a transfer token.
        Token transferToken =
                payer.createTransferToken(100.0, "EUR")
                        .setAccountId(payer.getAccountsBlocking().get(0).id())
                        .setToMemberId(payeeId)
                        .setDescription("Invoice payment")
                        .addAttachment(attachment)
                        .execute();

        // Payer endorses a token to a payee by signing it with her secure private key.
        transferToken = payer.endorseTokenBlocking(
                transferToken,
                Key.Level.STANDARD).getToken();

        Blob copyBlob = payer.getBlobBlocking(attachment.getBlobId());

        return transferToken;
    }
}
