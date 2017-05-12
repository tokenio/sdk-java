/**
 * Copyright (c) 2017 Token, Inc.
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

package io.token;

import static io.token.proto.common.transferinstructions.TransferInstructionsProtos.Source.SourceCase.BANKAUTHORIZATIONSOURCE;
import static io.token.proto.common.transferinstructions.TransferInstructionsProtos.Source.SourceCase.TOKENSOURCE;
import static io.token.util.Util.generateNonce;

import com.google.common.base.Strings;
import io.token.exceptions.TokenArgumentsException;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.proto.common.blob.BlobProtos.Attachment;
import io.token.proto.common.token.TokenProtos;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.proto.common.token.TokenProtos.TransferBody;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.Destination;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.Source.BankAuthorizationSource;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.Source.SourceCase;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.Source.TokenSource;

import rx.Observable;

/**
 * This class is used to build a transfer token. The required parameters are member, amount (which
 * is the lifetime amount of the token), and currency. One source of funds must be set: either
 * accountId or BankAuthorization. Finally, a redeemer must be set, specified by either username
 * or memberId.
 */
public final class TransferTokenBuilder {
    private final MemberAsync member;
    private final TokenPayload.Builder payload;

    /**
     * Creates the builder object.
     *
     * @param member payer of the token
     * @param amount lifetime amount of the token
     * @param currency currency of the token
     */
    public TransferTokenBuilder(MemberAsync member, double amount, String currency) {
        this.member = member;
        this.payload = TokenPayload.newBuilder()
                .setVersion("1.0")
                .setNonce(generateNonce())
                .setFrom(TokenProtos.TokenMember.newBuilder()
                        .setId(member.memberId())
                        .build())
                .setTransfer(TransferBody.newBuilder()
                        .setCurrency(currency)
                        .setLifetimeAmount(Double.toString(amount))
                        .build());
    }

    /**
     * Adds a source accountId to the token.
     *
     * @param accountId source accountId
     * @return builder
     */
    public TransferTokenBuilder setAccountId(String accountId) {
        payload.getTransferBuilder()
                .getInstructionsBuilder()
                .getSourceBuilder()
                .setTokenSource(TokenSource.newBuilder()
                        .setAccountId(accountId)
                        .setMemberId(member.memberId())
                        .build());
        return this;
    }

    /**
     * Sets the Bank Authorization.
     *
     * @param bankAuthorization BankAuthorization
     * @return builder
     */
    public TransferTokenBuilder setBankAuthorization(BankAuthorization bankAuthorization) {
        payload.getTransferBuilder()
                .getInstructionsBuilder()
                .getSourceBuilder()
                .setBankAuthorizationSource(BankAuthorizationSource.newBuilder()
                        .setBankAuthorization(bankAuthorization)
                        .build());
        return this;
    }

    /**
     * Sets the expiration date.
     *
     * @param expiresAtMs expiration date in ms.
     * @return builder
     */
    public TransferTokenBuilder setExpiresAtMs(long expiresAtMs) {
        payload.setExpiresAtMs(expiresAtMs);
        return this;
    }

    /**
     * Sets the effective date.
     *
     * @param effectiveAtMs effective date in ms.
     * @return builder
     */
    public TransferTokenBuilder setEffectiveAtMs(long effectiveAtMs) {
        payload.setExpiresAtMs(effectiveAtMs);
        return this;
    }

    /**
     * Sets the maximum amount per charge.
     *
     * @param chargeAmount amount
     * @return builder
     */
    public TransferTokenBuilder setChargeAmount(double chargeAmount) {
        payload.getTransferBuilder()
                .setAmount(Double.toString(chargeAmount));
        return this;
    }

    /**
     * Sets the description.
     *
     * @param description description
     * @return builder
     */
    public TransferTokenBuilder setDescription(String description) {
        payload.setDescription(description);
        return this;
    }

    /**
     * Adds a transfer destination.
     *
     * @param destination destination
     * @return builder
     */
    public TransferTokenBuilder addDestination(Destination destination) {
        payload.getTransferBuilder()
                .getInstructionsBuilder()
                .addDestinations(destination);
        return this;
    }

    /**
     * Sets the username of the redeemer.
     *
     * @param redeemerUsername username
     * @return builder
     */
    public TransferTokenBuilder setRedeemerUsername(String redeemerUsername) {
        payload.getTransferBuilder()
                .getRedeemerBuilder()
                .setUsername(redeemerUsername);
        return this;
    }

    /**
     * Sets the memberId of the redeemer.
     *
     * @param redeemerMemberId memberId
     * @return builder
     */
    public TransferTokenBuilder setRedeemerMemberId(String redeemerMemberId) {
        payload.getTransferBuilder()
                .getRedeemerBuilder()
                .setId(redeemerMemberId);
        return this;
    }

    /**
     * Adds an attachment to the token.
     *
     * @param attachment attachment
     * @return builder
     */
    public TransferTokenBuilder addAttachment(Attachment attachment) {
        payload.getTransferBuilder()
                .addAttachments(attachment);
        return this;
    }

    /**
     * Sets the username of the payee.
     *
     * @param toUsername username
     * @return builder
     */
    public TransferTokenBuilder setToUsername(String toUsername) {
        payload.getToBuilder().setUsername(toUsername);
        return this;
    }

    /**
     * Sets the memberId of the payee.
     *
     * @param toMemberId memberId
     * @return builder
     */
    public TransferTokenBuilder setToMemberId(String toMemberId) {
        payload.getToBuilder().setId(toMemberId);
        return this;
    }

    /**
     * Executes the request, creating a token.
     *
     * @return Token
     */
    public Token execute() {
        return executeAsync().toBlocking().single();
    }

    /**
     * Executes the request asynchronously.
     *
     * @return Token
     */
    public Observable<Token> executeAsync() {
        SourceCase sourceCase = payload.getTransfer().getInstructions().getSource().getSourceCase();
        if (sourceCase != BANKAUTHORIZATIONSOURCE && sourceCase != TOKENSOURCE) {
            throw new TokenArgumentsException("No source on token");
        }
        if (Strings.isNullOrEmpty(payload.getTransfer().getRedeemer().getId())
                && Strings.isNullOrEmpty(payload.getTransfer().getRedeemer().getUsername())) {
            throw new TokenArgumentsException("No redeemer on token");
        }

        return member.createTransferToken(payload.build());
    }
}
