package io.token.sample;

import io.token.Member;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.Destination;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.Destination.SepaDestination;

/**
 * Redeems a transfer token.
 */
public final class RedeemTransferTokenSample {
    /**
     * Redeems a transfer token to transfer money from payer bank account to payee bank account.
     *
     * @param payee payee Token member
     * @param tokenId ID of the token to redeem
     * @return a transfer Transfer
     */
    public static Transfer redeemTransferToken(Member payee, String tokenId) {
        // Retrieve a transfer token to redeem.
        Token transferToken = payee.getToken(tokenId);

        // Set the destination of the transfer
        Destination destination = Destination.newBuilder()
                .setSepaDestination(SepaDestination.newBuilder()
                        .setIban("iban123"))
                .build();

        // Payee redeems a transfer token. Money is transferred to a payee bank account.
        Transfer transfer = payee.redeemToken(transferToken, destination);


        return transfer;
    }
}
