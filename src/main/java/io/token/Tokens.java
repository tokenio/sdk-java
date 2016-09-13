package io.token;

import io.token.proto.common.token.TokenProtos.PaymentToken;
import io.token.proto.common.token.TokenProtos.TokenMember;

import static io.token.util.Util.generateNonce;

public interface Tokens {
    static PaymentToken newToken(
            Member member,
            double amount,
            String currencyCode) {
        return PaymentToken.newBuilder()
                .setNonce(generateNonce())
                .setPayer(TokenMember.newBuilder()
                        .setId(member.getMemberId()))
                .setCurrency(currencyCode)
                .setAmount(amount)
                .build();
    }
}
