package io.token.sample;

import static java.time.temporal.ChronoUnit.DAYS;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferDestination;
import io.token.user.Member;
import io.token.user.PrepareTokenResult;
import io.token.user.StandingOrderTokenBuilder;
import io.token.user.util.Util;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;

/**
 * Creates a standing order token to a payee.
 */
public final class CreateStandingOrderTokenSample {
    /**
     * Creates a transfer token and authorizes a money transfer from a payer to a payee.
     *
     * @param payer payer Token member
     * @param payeeAlias payee Token member alias
     * @param keyLevel the level of signature to provide
     * @return a transfer Token
     */
    public static Token createStandingOrderToken(
            Member payer,
            Alias payeeAlias,
            Key.Level keyLevel) {
        // We'll use this as a reference ID. Normally, a payer who
        // explicitly sets a reference ID would use an ID from a db.
        // E.g., a bill-paying service might use ID of a "purchase".
        // We don't have a db, so we fake it with a random string:
        String purchaseId = Util.generateNonce();

        // Set SEPA destination.
        TransferDestination sepaDestination = TransferDestination
                .newBuilder()
                .setSepa(TransferDestination.Sepa.newBuilder()
                        .setBic("XUIWC2489")
                        .setIban("DE89 3704 0044 0532 0130 00"))
                .build();

        DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd");

        // Set the details of the token.
        StandingOrderTokenBuilder builder = payer.createStandingOrderTokenBuilder(
                10.0, // amount
                "EUR", // currency
                "DAIL", // frequency of payment execution (daily)
                dateFormat.format(Date.from(Instant.now())), // start date
                dateFormat.format(Date.from(Instant.now().plus(7, DAYS)))) // end date
                // source account:
                .setAccountId(payer.getAccountsBlocking().get(0).id())
                // payee token alias:
                .setToAlias(payeeAlias)
                // optional description:
                .setDescription("Credit card statement payment")
                // ref ID (if not set, will get random ID)
                .setRefId(purchaseId)
                // make payments to SEPA destination
                .addDestination(sepaDestination);

        // Get the token redemption policy and resolve the token payload.
        PrepareTokenResult result = payer.prepareStandingOrderTokenBlocking(builder);

        // Create the token: Default behavior is to provide the member's signature
        // at the specified level. In other cases, it may be necessary to provide
        // additional signatures with payer.createToken(payload, signatures).
        Token standingOrderToken = payer.createTokenBlocking(result.getTokenPayload(), keyLevel);

        return standingOrderToken;
    }
}
