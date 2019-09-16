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

import static io.token.proto.common.token.TokenProtos.TokenRequestPayload.RequestBodyCase.BULK_TRANSFER_BODY;
import static io.token.util.Util.generateNonce;

import com.google.common.base.Preconditions;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos.ActingAs;
import io.token.proto.common.token.TokenProtos.BulkTransferBody;
import io.token.proto.common.token.TokenProtos.TokenMember;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.proto.common.token.TokenProtos.TokenRequest;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferEndpoint;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BulkTransferTokenBuilder {
    private static final Logger logger = LoggerFactory
            .getLogger(BulkTransferTokenBuilder.class);
    private static final int REF_ID_MAX_LENGTH = 18;

    private final TokenPayload.Builder payload;

    /**
     * Creates the builder object.
     *
     * @param member payer of the token
     * @param transfers list of transfers
     * @param totalAmount total amount irrespective of currency. Used for redundancy check.
     * @param source source account for all transfer
     */
    public BulkTransferTokenBuilder(
            Member member,
            List<BulkTransferBody.Transfer> transfers,
            double totalAmount,
            TransferEndpoint source) {
        this.payload = TokenPayload.newBuilder()
                .setVersion("1.0")
                .setFrom(TokenMember.newBuilder().setId(member.memberId()))
                .setBulkTransfer(BulkTransferBody.newBuilder()
                        .addAllTransfers(transfers)
                        .setTotalAmount(Double.toString(totalAmount))
                        .setSource(source));
        List<Alias> aliases = member.aliases().blockingSingle();
        if (!aliases.isEmpty()) {
            payload.getFromBuilder().setAlias(aliases.get(0));
        }
    }

    /**
     * Creates the builder object from a token request.
     *
     * @param tokenRequest token request
     */
    public BulkTransferTokenBuilder(TokenRequest tokenRequest) {
        if (tokenRequest.getRequestPayload().getRequestBodyCase() != BULK_TRANSFER_BODY) {
            throw new IllegalArgumentException(
                    "Require token request with bulk transfer body.");
        }
        if (!tokenRequest.getRequestPayload().hasTo()) {
            throw new IllegalArgumentException("No payee on token request");
        }
        BulkTransferBody body = tokenRequest.getRequestPayload()
                .getBulkTransferBody();
        this.payload = TokenPayload.newBuilder()
                .setVersion("1.0")
                .setRefId(tokenRequest.getRequestPayload().getRefId())
                .setFrom(tokenRequest.getRequestOptions().getFrom())
                .setTo(tokenRequest.getRequestPayload().getTo())
                .setDescription(tokenRequest.getRequestPayload().getDescription())
                .setReceiptRequested(tokenRequest.getRequestOptions().getReceiptRequested())
                .setTokenRequestId(tokenRequest.getId())
                .setBulkTransfer(body);
        if (tokenRequest.getRequestPayload().hasActingAs()) {
            this.payload.setActingAs(tokenRequest.getRequestPayload().getActingAs());
        }
    }

    /**
     * Sets the expiration date.
     *
     * @param expiresAtMs expiration date in ms.
     * @return builder
     */
    public BulkTransferTokenBuilder setExpiresAtMs(long expiresAtMs) {
        payload.setExpiresAtMs(expiresAtMs);
        return this;
    }

    /**
     * Sets the effective date.
     *
     * @param effectiveAtMs effective date in ms.
     * @return builder
     */
    public BulkTransferTokenBuilder setEffectiveAtMs(long effectiveAtMs) {
        payload.setEffectiveAtMs(effectiveAtMs);
        return this;
    }

    /**
     * Sets the time after which endorse is no longer possible.
     *
     * @param endorseUntilMs endorse until, in milliseconds.
     * @return builder
     */
    public BulkTransferTokenBuilder setEndorseUntilMs(long endorseUntilMs) {
        payload.setEndorseUntilMs(endorseUntilMs);
        return this;
    }

    /**
     * Sets the description.
     *
     * @param description description
     * @return builder
     */
    public BulkTransferTokenBuilder setDescription(String description) {
        payload.setDescription(description);
        return this;
    }

    /**
     * Adds a transfer source.
     *
     * @param source the source
     * @return builder
     */
    public BulkTransferTokenBuilder setSource(TransferEndpoint source) {
        payload.getBulkTransferBuilder().setSource(source);
        return this;
    }

    /**
     * Adds a linked source account to the token.
     *
     * @param accountId source accountId
     * @return builder
     */
    public BulkTransferTokenBuilder setAccountId(String accountId) {
        Preconditions.checkState(!payload.getFrom().getId().isEmpty());
        setSource(TransferEndpoint.newBuilder()
                .setAccount(BankAccount.newBuilder()
                        .setToken(BankAccount.Token.newBuilder()
                                .setAccountId(accountId)
                                .setMemberId(payload.getFrom().getId())))
                .build());
        return this;
    }

    /**
     * Sets the alias of the payee.
     *
     * @param toAlias alias
     * @return builder
     */
    public BulkTransferTokenBuilder setToAlias(Alias toAlias) {
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
    public BulkTransferTokenBuilder setToMemberId(String toMemberId) {
        payload.getToBuilder().setId(toMemberId);
        return this;
    }

    /**
     * Sets the reference ID of the token.
     *
     * @param refId the reference Id, at most 18 characters long
     * @return builder
     */
    public BulkTransferTokenBuilder setRefId(String refId) {
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
     * Sets acting as on the token.
     *
     * @param actingAs entity the redeemer is acting on behalf of
     * @return builder
     */
    public BulkTransferTokenBuilder setActingAs(ActingAs actingAs) {
        payload.setActingAs(actingAs);
        return this;
    }

    /**
     * Sets the token request ID.
     *
     * @param tokenRequestId token request id
     * @return builder
     */
    public BulkTransferTokenBuilder setTokenRequestId(String tokenRequestId) {
        payload.setTokenRequestId(tokenRequestId);
        return this;
    }

    /**
     * Sets the flag indicating whether a receipt is requested.
     *
     * @param receiptRequested receipt requested flag
     * @return builder
     */
    public BulkTransferTokenBuilder setReceiptRequested(boolean receiptRequested) {
        payload.setReceiptRequested(receiptRequested);
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
