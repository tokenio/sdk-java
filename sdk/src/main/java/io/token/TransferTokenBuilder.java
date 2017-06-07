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

import static io.token.proto.common.account.AccountProtos.BankAccount.AccountCase.TOKEN;
import static io.token.proto.common.account.AccountProtos.BankAccount.AccountCase.TOKEN_AUTHORIZATION;
import static io.token.util.Util.generateNonce;

import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import io.token.exceptions.TokenArgumentsException;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.account.AccountProtos.BankAccount.AccountCase;
import io.token.proto.common.account.AccountProtos.BankAccount.TokenAuthorization;
import io.token.proto.common.blob.BlobProtos.Attachment;
import io.token.proto.common.blob.BlobProtos.Blob.Payload;
import io.token.proto.common.pricing.PricingProtos;
import io.token.proto.common.pricing.PricingProtos.Pricing;
import io.token.proto.common.pricing.PricingProtos.PricingInstructions;
import io.token.proto.common.token.TokenProtos;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.proto.common.token.TokenProtos.TransferBody;
import io.token.proto.common.transfer.TransferProtos;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.PurposeOfPayment;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferEndpoint;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Func1;

/**
 * This class is used to build a transfer token. The required parameters are member, amount (which
 * is the lifetime amount of the token), and currency. One source of funds must be set: either
 * accountId or BankAuthorization. Finally, a redeemer must be set, specified by either username
 * or memberId.
 */
public final class TransferTokenBuilder {
    private static final Logger logger = LoggerFactory.getLogger(TransferTokenBuilder.class);

    private final MemberAsync member;
    private final TokenPayload.Builder payload;

    // Used for attaching files / data to tokens
    private final List<Payload> blobPayloads;

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
                .setFrom(TokenProtos.TokenMember.newBuilder()
                        .setId(member.memberId())
                        .build())
                .setTransfer(TransferBody.newBuilder()
                        .setCurrency(currency)
                        .setLifetimeAmount(Double.toString(amount))
                        .build());
        blobPayloads = new ArrayList<>();
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
                .setAccount(BankAccount.newBuilder()
                        .setToken(BankAccount.Token.newBuilder()
                                .setAccountId(accountId)
                                .setMemberId(member.memberId()))
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
                .setAccount(BankAccount.newBuilder()
                        .setTokenAuthorization(TokenAuthorization.newBuilder()
                                .setAuthorization(bankAuthorization))
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
    public TransferTokenBuilder addDestination(TransferEndpoint destination) {
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
     * Adds an attachment by filename (reads file, uploads it, and attaches it).
     *
     * @param ownerId id of the owner of the file
     * @param type MIME type of file
     * @param name name of the file
     * @param data file binary data
     * @return builder
     */
    public TransferTokenBuilder addAttachment(
            String ownerId,
            String type,
            String name,
            byte[] data) {
        blobPayloads.add(Payload
                .newBuilder()
                .setOwnerId(ownerId)
                .setType(type)
                .setName(name)
                .setData(ByteString.copyFrom(data))
                .build());
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
     * Sets the referenceId of the token.
     *
     * @param refId referenceId
     * @return builder
     */
    public TransferTokenBuilder setRefId(String refId) {
        payload.setRefId(refId);
        return this;
    }

    /**
     * Sets the pricing (fees/fx) on the token.
     *
     * @param pricing pricing
     * @return builder
     */
    public TransferTokenBuilder setPricing(Pricing pricing) {
        payload.getTransferBuilder().setPricing(pricing);
        return this;
    }

    /**
     * Sets the purpose of payment.
     *
     * @param purposeOfPayment purpose of payment
     * @return builder
     */
    public TransferTokenBuilder setPurposeOfPayment(PurposeOfPayment purposeOfPayment) {
        payload.getTransferBuilder().getInstructionsBuilder().setTransferPurpose(purposeOfPayment);
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
        AccountCase sourceCase =
                payload.getTransfer().getInstructions().getSource().getAccount().getAccountCase();
        if (sourceCase != TOKEN_AUTHORIZATION && sourceCase != TOKEN) {
            throw new TokenArgumentsException("No source on token");
        }
        if (Strings.isNullOrEmpty(payload.getTransfer().getRedeemer().getId())
                && Strings.isNullOrEmpty(payload.getTransfer().getRedeemer().getUsername())) {
            throw new TokenArgumentsException("No redeemer on token");
        }

        if (payload.getRefId().isEmpty()) {
            logger.warn("refId is not set. A random ID will be used.");
            payload.setRefId(generateNonce());
        }

        List<Observable<Attachment>> attachmentUploads = new ArrayList<>();

        for (Payload p : blobPayloads) {
            attachmentUploads.add(member.createBlob(
                    p.getOwnerId(),
                    p.getType(),
                    p.getName(),
                    p.getData().toByteArray()));
        }

        return Observable.merge(attachmentUploads)
                .toList()
                .flatMap(new Func1<List<Attachment>, Observable<Token>>() {
                    public Observable<Token> call(List<Attachment> attachments) {
                        payload.getTransferBuilder().addAllAttachments(attachments);
                        return member.createTransferToken(payload.build());
                    }
                });
    }
}
