package io.token.sample;

import io.token.proto.common.submission.SubmissionProtos.StandingOrderSubmission;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.tpp.Member;

/**
 * Redeems a standing order token.
 */
public final class RedeemStandingOrderTokenSample {
    /**
     * Redeems a standing order token to make a series of transfers from payer bank account
     * to payee bank account.
     *
     * @param payee payee Token member
     * @param tokenId ID of the token to redeem
     * @return standing order submission record
     */
    public static StandingOrderSubmission redeemStandingOrderToken(
            Member payee,
            String tokenId) { // ID of token to redeem
        // Retrieve a standing order to redeem.
        Token token = payee.getTokenBlocking(tokenId);

        // Payee redeems a standing order token.
        // Money is transferred to a payee bank account.
        StandingOrderSubmission submission = payee.redeemStandingOrderTokenBlocking(token.getId());

        return submission;
    }
}
