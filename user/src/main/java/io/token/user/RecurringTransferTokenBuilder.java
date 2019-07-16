/**
 * Copyright (c) 2019 Token, Inc.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.token.user;

import static io.token.proto.common.account.AccountProtos.BankAccount.AccountCase.BANK;
import static io.token.proto.common.account.AccountProtos.BankAccount.AccountCase.TOKEN;
import static io.token.proto.common.token.TokenProtos.TokenPayload.BodyCase.TRANSFER;
import static io.token.proto.common.token.TokenProtos.TokenRequestPayload.RequestBodyCase.RECURRING_TRANSFER_BODY;
import static io.token.proto.common.token.TokenProtos.TokenRequestPayload.RequestBodyCase.TRANSFER_BODY;
import static io.token.util.Util.generateNonce;

import io.reactivex.Observable;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.account.AccountProtos.BankAccount.AccountCase;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.providerspecific.ProviderSpecific.ProviderTransferMetadata;
import io.token.proto.common.token.TokenProtos;
import io.token.proto.common.token.TokenProtos.ActingAs;
import io.token.proto.common.token.TokenProtos.RecurringTransferBody;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenMember;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.proto.common.token.TokenProtos.TokenRequest;
import io.token.proto.common.token.TokenProtos.TokenRequestPayload;
import io.token.proto.common.token.TokenProtos.TransferBody;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.PurposeOfPayment;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferDestination;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferEndpoint;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferInstructions;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to build a transfer token. The required parameters are member, amount (which
 * is the lifetime amount of the token), and currency. One source of funds must be set: either
 * accountId or BankAuthorization. Finally, a redeemer must be set, specified by either alias
 * or memberId.
 */
public final class RecurringTransferTokenBuilder {
    private static final Logger logger = LoggerFactory
            .getLogger(RecurringTransferTokenBuilder.class);
    private static final int REF_ID_MAX_LENGTH = 18;

    private final Member member;
    private final TokenPayload.Builder payload;

    /**
     * Creates the builder object.
     *
     * @param member payer of the token
     * @param amount amount per charge of the recurring transfer token
     * @param currency currency of the token
     * @param frequency ISO 20022 code for the frequency of the recurring payment:
     *                  DAIL, WEEK, TOWK, MNTH, TOMN, QUTR, SEMI, YEAR
     * @param startDate start date of the recurring payment: ISO 8601 YYYY-MM-DD or YYYYMMDD
     * @param endDate end date of the recurring payment: ISO 8601 YYYY-MM-DD or YYYYMMDD
     */
    public RecurringTransferTokenBuilder(
            Member member,
            double amount,
            String currency,
            String frequency,
            String startDate,
            Optional<String> endDate) {
        this.member = member;
        this.payload = TokenPayload.newBuilder()
                .setVersion("1.0")
                .setRecurringTransfer(RecurringTransferBody.newBuilder()
                        .setCurrency(currency)
                        .setAmount(Double.toString(amount))
                        .setFrequency(frequency)
                        .setStartDate(startDate)
                        .setEndDate(endDate.orElse("")));

        if (member != null) {
            from(member.memberId());
            List<Alias> aliases = member.aliases().blockingSingle();
            if (!aliases.isEmpty()) {
                payload.getFromBuilder().setAlias(aliases.get(0));
            }
        }
    }

    /**
     * Creates the builder object from a token request.
     *
     * @param member payer of the token
     * @param tokenRequest token request
     */
    public RecurringTransferTokenBuilder(Member member, TokenRequest tokenRequest) {
        if (tokenRequest.getRequestPayload().getRequestBodyCase() != RECURRING_TRANSFER_BODY) {
            throw new IllegalArgumentException(
                    "Require token request with recurring transfer body.");
        }
        if (!tokenRequest.getRequestPayload().hasTo()) {
            throw new IllegalArgumentException("No payee on token request");
        }
        this.member = member;
        RecurringTransferBody transferBody = tokenRequest.getRequestPayload()
                .getRecurringTransferBody();
        this.payload = TokenPayload.newBuilder()
                .setVersion("1.0")
                .setRefId(tokenRequest.getRequestPayload().getRefId())
                .setFrom(tokenRequest.getRequestOptions().hasFrom()
                        ? tokenRequest.getRequestOptions().getFrom()
                        : TokenMember.newBuilder().setId(member.memberId()).build())
                .setTo(tokenRequest.getRequestPayload().getTo())
                .setDescription(tokenRequest.getRequestPayload().getDescription())
                .setReceiptRequested(tokenRequest.getRequestOptions().getReceiptRequested())
                .setTokenRequestId(tokenRequest.getId())
                .setRecurringTransfer(transferBody);
        if (tokenRequest.getRequestPayload().hasActingAs()) {
            this.payload.setActingAs(tokenRequest.getRequestPayload().getActingAs());
        }
    }

    /**
     * Adds a source accountId to the token.
     *
     * @param accountId source accountId
     * @return builder
     */
    public RecurringTransferTokenBuilder setAccountId(String accountId) {
        payload.getRecurringTransferBuilder()
                .getInstructionsBuilder()
                .getSourceBuilder()
                .setAccount(BankAccount.newBuilder()
                        .setToken(BankAccount.Token.newBuilder()
                                .setAccountId(accountId)
                                .setMemberId(member.memberId()))
                        .build());
        return this;
    }

    /**
     * Sets the source custom authorization.
     *
     * @param bankId source bank ID
     * @param authorization source custom authorization
     * @return builder
     */
    public RecurringTransferTokenBuilder setCustomAuthorization(
            String bankId,
            String authorization) {
        payload.getRecurringTransferBuilder()
                .getInstructionsBuilder()
                .getSourceBuilder()
                .setAccount(BankAccount.newBuilder()
                        .setCustom(BankAccount.Custom.newBuilder()
                                .setBankId(bankId)
                                .setPayload(authorization)
                                .build())
                        .build());
        return this;
    }

    /**
     * Sets the expiration date.
     *
     * @param expiresAtMs expiration date in ms.
     * @return builder
     */
    public RecurringTransferTokenBuilder setExpiresAtMs(long expiresAtMs) {
        payload.setExpiresAtMs(expiresAtMs);
        return this;
    }

    /**
     * Sets the effective date.
     *
     * @param effectiveAtMs effective date in ms.
     * @return builder
     */
    public RecurringTransferTokenBuilder setEffectiveAtMs(long effectiveAtMs) {
        payload.setEffectiveAtMs(effectiveAtMs);
        return this;
    }

    /**
     * Sets the time after which endorse is no longer possible.
     *
     * @param endorseUntilMs endorse until, in milliseconds.
     * @return builder
     */
    public RecurringTransferTokenBuilder setEndorseUntilMs(long endorseUntilMs) {
        payload.setEndorseUntilMs(endorseUntilMs);
        return this;
    }

    /**
     * Sets the description.
     *
     * @param description description
     * @return builder
     */
    public RecurringTransferTokenBuilder setDescription(String description) {
        payload.setDescription(description);
        return this;
    }

    /**
     * Adds a transfer source.
     *
     * @param source the source
     * @return builder
     */
    public RecurringTransferTokenBuilder setSource(TransferEndpoint source) {
        payload.getRecurringTransferBuilder()
                .getInstructionsBuilder()
                .setSource(source);
        return this;
    }

    /**
     * Adds a transfer destination.
     *
     * @param destination destination
     * @return builder
     */
    @Deprecated
    public RecurringTransferTokenBuilder addDestination(TransferEndpoint destination) {
        payload.getRecurringTransferBuilder()
                .getInstructionsBuilder()
                .addDestinations(destination);
        return this;
    }

    /**
     * Adds a transfer destination.
     *
     * @param destination destination
     * @return builder
     */
    public RecurringTransferTokenBuilder addDestination(TransferDestination destination) {
        payload.getRecurringTransferBuilder()
                .getInstructionsBuilder()
                .addTransferDestinations(destination);
        return this;
    }

    /**
     * Sets the alias of the payee.
     *
     * @param toAlias alias
     * @return builder
     */
    public RecurringTransferTokenBuilder setToAlias(Alias toAlias) {
        payload.getToBuilder()
                .setAlias(toAlias);
        return this;
    }

    /**
     * Sets the memberId of the payee.
     *
     * @param toMemberId memberId
     * @return builder
     */
    public RecurringTransferTokenBuilder setToMemberId(String toMemberId) {
        payload.getToBuilder().setId(toMemberId);
        return this;
    }

    /**
     * Sets the reference ID of the token.
     *
     * @param refId the reference Id, at most 18 characters long
     * @return builder
     */
    public RecurringTransferTokenBuilder setRefId(String refId) {
        if (refId.length() > REF_ID_MAX_LENGTH) {
            throw new IllegalArgumentException(String.format(
                    "The length of the refId is at most %s, got: %s",
                    REF_ID_MAX_LENGTH,
                    refId.length()));
        }
        payload.setRefId(refId);
        return this;
    }

    /**
     * Sets the purpose of payment.
     *
     * @param purposeOfPayment purpose of payment
     * @return builder
     */
    public RecurringTransferTokenBuilder setPurposeOfPayment(PurposeOfPayment purposeOfPayment) {
        payload
                .getRecurringTransferBuilder()
                .getInstructionsBuilder()
                .getMetadataBuilder()
                .setTransferPurpose(purposeOfPayment);
        return this;
    }

    /**
     * Sets acting as on the token.
     *
     * @param actingAs entity the redeemer is acting on behalf of
     * @return builder
     */
    public RecurringTransferTokenBuilder setActingAs(ActingAs actingAs) {
        payload.setActingAs(actingAs);
        return this;
    }

    /**
     * Sets the token request ID.
     *
     * @param tokenRequestId token request id
     * @return builder
     */
    public RecurringTransferTokenBuilder setTokenRequestId(String tokenRequestId) {
        payload.setTokenRequestId(tokenRequestId);
        return this;
    }

    /**
     * Sets the flag indicating whether a receipt is requested.
     *
     * @param receiptRequested receipt requested flag
     * @return builder
     */
    public RecurringTransferTokenBuilder setReceiptRequested(boolean receiptRequested) {
        payload.setReceiptRequested(receiptRequested);
        return this;
    }

    /**
     * Sets provider transfer metadata.
     *
     * @param metadata the metadata
     * @return the provider transfer metadata
     */
    public RecurringTransferTokenBuilder setProviderTransferMetadata(
            ProviderTransferMetadata metadata) {
        payload.getRecurringTransferBuilder()
                .getInstructionsBuilder()
                .getMetadataBuilder()
                .setProviderTransferMetadata(metadata);
        return this;
    }

    RecurringTransferTokenBuilder from(String memberId) {
        payload.setFrom(TokenMember.newBuilder().setId(memberId));
        return this;
    }

    /**
     * Builds a token payload, without uploading blobs or attachments.
     *
     * @return token payload
     */
    public TokenPayload buildPayload() {
        if (payload.getRefId().isEmpty()) {
            logger.warn("refId is not set. A random ID will be used.");
            payload.setRefId(generateNonce());
        }
        return payload.build();
    }
}
