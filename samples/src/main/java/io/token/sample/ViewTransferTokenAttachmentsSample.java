package io.token.sample;

import io.token.Destinations;
import io.token.Member;
import io.token.proto.common.blob.BlobProtos.Attachment;
import io.token.proto.common.blob.BlobProtos.Blob;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transfer.TransferProtos.Transfer;

import java.util.List;

/**
 * Fetches attachments from a transfer token.
 */
public final class ViewTransferTokenAttachmentsSample {
    private static void showImage(String name, String mimeType, byte[] data) {
        // no-op fake to make example look plausible
    }

    /**
     * Show how to download attachment data from a token
     * @param payee Payee member.
     * @param tokenId Token with attachments we want to show
     */
    public static void displayAttachmentFromTransferToken(
            Member payee,
            String tokenId) {
        // Retrieve a transfer token to redeem.
        Token transferToken = payee.getToken(tokenId);

        List<Attachment> attachments = transferToken
                .getPayload()
                .getTransfer()
                .getAttachmentsList();
        for (Attachment attachment : attachments) {
            // Attachment has some metadata (name, type) but not the "file" contents.
            if (attachment.getType().startsWith("image/")) {
                // Download the contents for the attachment[s] we want:
                Blob blob = payee.getTokenBlob(tokenId, attachment.getBlobId());
                // Use the attachment data.
                showImage(
                        blob.getPayload().getName(),  // "invoice.jpg"
                        blob.getPayload().getType(),  // MIME, e.g., "image/jpeg"
                        blob.getPayload().getData().toByteArray()); // byte[] of contents
            }
        }
    }
}
