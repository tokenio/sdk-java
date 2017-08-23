package io.token.sample;

import static io.token.TokenIO.TokenCluster.SANDBOX;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.token.TokenProtos.TokenMember;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.proto.common.token.TokenProtos.TransferBody;

/**
 * Creates a payment request (a transfer token payload) and sends it to a potential payer.
 */
public final class NotifyPaymentRequestSample {
    /**
     * Creates a payment request (a transfer token payload) and sends it to a potential payer.
     *
     * @param payee payer Token member
     * @param payerAlias payee Token member alias
     * @return a transfer Token
     */
    public static NotifyStatus notifyPaymentRequest(
            Member payee,
            Alias payerAlias) {

        TokenPayload paymentRequest = TokenPayload.newBuilder()
                .setDescription("Sample payment request")
                .setFrom(TokenMember.newBuilder()
                        .setAlias(payerAlias))
                .setTo(TokenMember.newBuilder()
                        .setAlias(payee.firstAlias()))
                .setTransfer(TransferBody.newBuilder()
                        .setAmount("100.00")
                        .setCurrency("EUR"))
                .build();

        try (TokenIO tokenIO = TokenIO.create(SANDBOX)) {
            NotifyStatus status = tokenIO.notifyPaymentRequest(
                    payerAlias,
                    paymentRequest);
            return status;
        }
    }
}
