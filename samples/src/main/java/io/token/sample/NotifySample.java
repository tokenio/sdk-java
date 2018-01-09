package io.token.sample;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.notification.NotificationProtos.RequestStepUp.RequestType;
import io.token.proto.common.token.TokenProtos.TokenMember;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.proto.common.token.TokenProtos.TransferBody;
import io.token.util.Util;

public class NotifySample {
    /**
     * Creates a payment request (a transfer token payload)
     * and sends it to a potential payer.
     *
     * @param tokenIO initialized SDK
     * @param payee payer Token member
     * @param payerAlias payee Token member alias
     * @return a transfer Token
     */
    public static NotifyStatus notifyPaymentRequest(
            TokenIO tokenIO,
            Member payee,
            Alias payerAlias) {
        // We'll use this as a reference ID. Normally, a payee who
        // explicitly sets a reference ID would use an ID from a db.
        // E.g., an online merchant might use the ID of a "shopping cart".
        // We don't have a db, so we fake it with a random string:
        String cartId = Util.generateNonce();
        TokenPayload paymentRequest = TokenPayload.newBuilder()
                .setDescription("Sample payment request")
                .setFrom(TokenMember.newBuilder()
                        .setAlias(payerAlias))
                .setTo(TokenMember.newBuilder()
                        .setAlias(payee.firstAlias()))
                .setTransfer(TransferBody.newBuilder()
                        .setAmount("100.00")
                        .setCurrency("EUR"))
                // if refID not set, the eventually-created
                // transfer token will have random refId:
                .setRefId(cartId)
                .build();

        NotifyStatus status = tokenIO.notifyPaymentRequest(paymentRequest);
        return status;
    }

    /**
     * Triggers a notification to step up the signature level when endorsing a token.
     *
     * @param member member
     * @param tokenId token id
     * @return notification status
     */
    public static NotifyStatus triggerTokenStepUpNotification(Member member, String tokenId) {
        return member.triggerTokenStepUpNotification(tokenId);
    }

    /**
     * Triggers a notification to step up the signature level when requesting information.
     *
     * @param member member
     * @param requestType request type
     * @return notification status
     */
    public static NotifyStatus triggerTokenStepUpNotification(
            Member member,
            RequestType requestType) {
        return member.triggerRequestStepUpNotification(requestType);
    }
}
