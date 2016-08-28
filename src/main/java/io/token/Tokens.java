package io.token;

import com.google.protobuf.StringValue;
import io.token.proto.common.token.TokenProtos;
import io.token.proto.common.token.TokenProtos.PaymentToken;

import static io.token.util.Util.generateNonce;

public interface Tokens {
    static PaymentToken newToken(
            Member member,
            double amount,
            String currencyCode) {
        return PaymentToken.newBuilder()
                .setNonce(generateNonce())
                .setPayer(TokenProtos.Member.newBuilder()
                        .setId(StringValue.newBuilder()
                                .setValue(member.getMemberId())
                                .build()))
                .setCurrency(currencyCode)
                .setAmount(amount)
                .build();
    }
}
