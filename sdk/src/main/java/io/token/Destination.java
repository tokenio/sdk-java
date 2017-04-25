package io.token;


import io.token.proto.common.transferinstructions.TransferInstructionsProtos;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.Destination.AchDestination;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.Destination.SepaDestination;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.Destination.SwiftDestination;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.Destination.TokenDestination;

import javax.annotation.Nullable;

public final class Destination {
    private Destination() {}

    /**
     * Creates a destination of type token (Paying to a token member account).
     *
     * @param accountId token accountId
     * @param memberId token memberId
     * @return Destination
     */
    public static TransferInstructionsProtos.Destination token(
            String accountId,
            @Nullable String memberId) {
        TokenDestination.Builder builder = TokenDestination.newBuilder()
                .setAccountId(accountId);
        if (memberId != null) {
            builder.setMemberId(memberId);
        }
        return TransferInstructionsProtos.Destination.newBuilder()
                .setTokenDestination(builder.build())
                .build();
    }

    /**
     * Creates a destination of type sepa.
     *
     * @param iban payee's iban
     * @return Destination
     */
    public static TransferInstructionsProtos.Destination sepa(String iban) {
        return TransferInstructionsProtos.Destination.newBuilder()
                .setSepaDestination(SepaDestination.newBuilder()
                        .setIban(iban)
                        .build())
                .build();
    }

    /**
     * Creates a destination of type swift.
     *
     * @param bic bank identification code
     * @param account account number
     * @return Destination
     */
    public static TransferInstructionsProtos.Destination swift(String bic, String account) {
        return TransferInstructionsProtos.Destination.newBuilder()
                .setSwiftDestination(SwiftDestination.newBuilder()
                        .setBic(bic)
                        .setAccount(account)
                        .build())
                .build();
    }

    /**
     * Creates a destination of type ACH.
     *
     * @param routing routing number
     * @param account account number
     * @return Destination
     */
    public static TransferInstructionsProtos.Destination ach(String routing, String account) {
        return TransferInstructionsProtos.Destination.newBuilder()
                .setAchDestination(AchDestination.newBuilder()
                        .setRouting(routing)
                        .setAccount(account)
                        .build())
                .build();
    }
}
