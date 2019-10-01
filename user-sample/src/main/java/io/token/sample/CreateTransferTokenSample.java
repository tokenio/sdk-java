package io.token.sample;

import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferDestination;
import io.token.user.Member;
import io.token.user.PrepareTokenResult;
import io.token.user.TransferTokenBuilder;
import io.token.user.util.Util;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Creates a transfer token to a payee.
 */
public final class CreateTransferTokenSample {
    /**
     * Creates a transfer token and authorizes a money transfer from a payer to a payee.
     *
     * @param payer payer Token member
     * @param payeeAlias payee Token member alias
     * @param keyLevel the level of signature to provide
     * @return a transfer Token
     */
    public static Token createTransferToken(
            Member payer,
            Alias payeeAlias,
            Key.Level keyLevel) {
        // We'll use this as a reference ID. Normally, a payer who
        // explicitly sets a reference ID would use an ID from a db.
        // E.g., a bill-paying service might use ID of a "purchase".
        // We don't have a db, so we fake it with a random string:
        String purchaseId = Util.generateNonce();

        // Set the details of the token.
        TransferTokenBuilder builder = payer.createTransferTokenBuilder(
                10.0, // amount
                "EUR")  // currency
                // source account:
                .setAccountId(payer.getAccountsBlocking().get(0).id())
                // payee token alias:
                .setToAlias(payeeAlias)
                // optional description:
                .setDescription("Book purchase")
                // ref ID (if not set, will get random ID)
                .setRefId(purchaseId);

        // Get the token redemption policy and resolve the token payload.
        PrepareTokenResult result = payer.prepareTransferTokenBlocking(builder);

        // Create the token: Default behavior is to provide the member's signature
        // at the specified level. In other cases, it may be necessary to provide
        // additional signatures with payer.createToken(payload, signatures).
        Token transferToken = payer.createTokenBlocking(result.getTokenPayload(), keyLevel);

        return transferToken;
    }

    /**
     * Creates a transfer token using some other options.
     *
     * @param payer payer Token member
     * @param payeeId payee Token member Id
     * @return a transfer Token
     */
    public static Token createTransferTokenWithOtherOptions(
            Member payer,
            String payeeId) {
        long now = System.currentTimeMillis();

        // Set the details of the token.
        TransferTokenBuilder builder = payer.createTransferTokenBuilder(
                120.0, // amount
                "EUR")  // currency
                // source account:
                .setAccountId(payer.getAccountsBlocking().get(0).id())
                .setToMemberId(payeeId)
                .setToMemberId(payeeId)
                // effective in one second:
                .setEffectiveAtMs(now + 1000)
                // expires in 300 seconds:
                .setExpiresAtMs(now + (300 * 1000))
                .setRefId("a713c8a61994a749")
                .setChargeAmount(10.0)
                .setDescription("Book purchase");

        // Get the token redemption policy and resolve the token payload.
        PrepareTokenResult result = payer.prepareTransferTokenBlocking(builder);

        // Create the token, signing with the payer's STANDARD-level key
        Token transferToken = payer.createTokenBlocking(result.getTokenPayload(), STANDARD);

        return transferToken;
    }

    /**
     * Creates transfer token to a destination.
     *
     * @param payer Payer who has no linked bank accounts
     * @param payeeAlias Alias of payee member
     * @return a transfer Token
     */
    public static Token createTransferTokenToDestination(
            Member payer,
            Alias payeeAlias) {
        // Set SEPA destination.
        TransferDestination sepaDestination = TransferDestination
                .newBuilder()
                .setSepa(TransferDestination.Sepa.newBuilder()
                        .setBic("XUIWC2489")
                        .setIban("DE89 3704 0044 0532 0130 00"))
                .build();

        // Set the destination and other details.
        TransferTokenBuilder builder =
                payer.createTransferTokenBuilder(
                        100.0, // amount
                        "EUR")  // currency
                        .setAccountId(payer.getAccountsBlocking().get(0).id())
                        .setToAlias(payeeAlias)
                        .addDestination(sepaDestination);

        // Get the token redemption policy and resolve the token payload.
        PrepareTokenResult result = payer.prepareTransferTokenBlocking(builder);

        // Create the token, signing with the payer's STANDARD-level key
        Token transferToken = payer.createTokenBlocking(result.getTokenPayload(), STANDARD);

        return transferToken;
    }

    /**
     * Creates a transfer token with a later execution date.
     *
     * @param payer payer Token member
     * @param payeeAlias payee Token member alias
     * @return a transfer Token
     */
    public static Token createTransferTokenScheduled(
            Member payer,
            Alias payeeAlias) {
        // We'll use this as a reference ID. Normally, a payer who
        // explicitly sets a reference ID would use an ID from a db.
        // E.g., a bill-paying service might use ID of a "purchase".
        // We don't have a db, so we fake it with a random string:
        String purchaseId = Util.generateNonce();

        DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd");

        // Set the details of the token.
        TransferTokenBuilder builder = payer.createTransferTokenBuilder(
                10.0, // amount
                "EUR")  // currency
                // source account:
                .setAccountId(payer.getAccountsBlocking().get(0).id())
                // payee token alias:
                .setToAlias(payeeAlias)
                // optional description:
                .setDescription("Book purchase")
                // ref ID (if not set, will get random ID)
                .setRefId(purchaseId)
                // set the transfer to execute in 30 days
                .setExecutionDate(dateFormat.format(
                        Date.from(Instant.now().plus(30, ChronoUnit.DAYS))));

        // Get the token redemption policy and resolve the token payload.
        PrepareTokenResult result = payer.prepareTransferTokenBlocking(builder);

        // Create the token: Default behavior is to provide the member's signature
        // at the specified level. In other cases, it may be necessary to provide
        // additional signatures with payer.createToken(payload, signatures).
        Token transferToken = payer.createTokenBlocking(result.getTokenPayload(), STANDARD);

        return transferToken;
    }
}
