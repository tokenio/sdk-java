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

public final class TokenBuilder {
    private final MemberAsync member;
    private final TokenPayload.Builder payload;

    public TokenBuilder(MemberAsync member, double amount, String currency) {
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

    public TokenBuilder setAccountId(String accountId) {
        payload.getTransferBuilder()
                .getInstructionsBuilder()
                .getSourceBuilder()
                .setTokenSource(TokenSource.newBuilder()
                        .setAccountId(accountId)
                        .setMemberId(member.memberId())
                        .build());
        return this;
    }

    public TokenBuilder setBankAuthorization(BankAuthorization bankAuthorization) {
        payload.getTransferBuilder()
                .getInstructionsBuilder()
                .getSourceBuilder()
                .setBankAuthorizationSource(BankAuthorizationSource.newBuilder()
                        .setBankAuthorization(bankAuthorization)
                        .build());
        return this;
    }

    public TokenBuilder setExpiresAtMs(long expiresAtMs) {
        payload.setExpiresAtMs(expiresAtMs);
        return this;
    }

    public TokenBuilder setEffectiveAtMs(long effectiveAtMs) {
        payload.setExpiresAtMs(effectiveAtMs);
        return this;
    }

    public TokenBuilder setChargeAmount(double chargeAmount) {
        payload.getTransferBuilder()
                .setAmount(Double.toString(chargeAmount));
        return this;
    }

    public TokenBuilder setDescription(String description) {
        payload.setDescription(description);
        return this;
    }

    public TokenBuilder addDestination(Destination destination) {
        payload.getTransferBuilder()
                .getInstructionsBuilder()
                .addDestinations(destination);
        return this;
    }

    public TokenBuilder setRedeemerUsername(String redeemerUsername) {
        payload.getTransferBuilder()
                .getRedeemerBuilder()
                .setUsername(redeemerUsername);
        return this;
    }

    public TokenBuilder setRedeemerMemberId(String redeemerMemberId) {
        payload.getTransferBuilder()
                .getRedeemerBuilder()
                .setId(redeemerMemberId);
        return this;
    }

    public TokenBuilder addAttachment(Attachment attachment) {
        payload.getTransferBuilder()
                .addAttachments(attachment);
        return this;
    }

    public Token execute() {
        return executeAsync().toBlocking().single();
    }

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
