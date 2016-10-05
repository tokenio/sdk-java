package io.token.asserts;

import io.token.Member;
import io.token.proto.common.token.TokenProtos.PaymentToken;
import io.token.proto.common.token.TokenProtos.TokenSignature.Action;
import io.token.security.SecretKey;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static io.token.proto.common.token.TokenProtos.TokenSignature.Action.*;
import static java.util.stream.Collectors.toList;

public final class TokenAssertion extends AbstractAssert<TokenAssertion, PaymentToken> {
    public static TokenAssertion assertThat(PaymentToken token) {
        return new TokenAssertion(token);
    }

    private TokenAssertion(PaymentToken actual) {
        super(actual, TokenAssertion.class);
    }

    public TokenAssertion hasPayer(Member member) {
        Assertions
                .assertThat(actual.getPayload().getPayer().getId())
                .isEqualTo(member.getMemberId());
        return this;
    }

    public TokenAssertion hasRedeemerAlias(String alias) {
        Assertions
                .assertThat(actual.getPayload().getRedeemer().getAlias())
                .isEqualTo(alias);
        return this;
    }

    public TokenAssertion hasAmount(double amount) {
        Assertions.assertThat(actual.getPayload().getAmount()).isEqualTo(Double.toString(amount));
        return this;
    }

    public TokenAssertion hasCurrency(String currency) {
        Assertions.assertThat(actual.getPayload().getCurrency()).isEqualTo(currency);
        return this;
    }

    public TokenAssertion hasNSignatures(int count) {
        Assertions.assertThat(actual.getSignaturesCount()).isEqualTo(count);
        return this;
    }

    public TokenAssertion isEndorsedBy(Member... members) {
        return hasKeySignatures(
                Arrays.stream(members)
                        .map(Member::getKey)
                        .collect(toList()),
                ENDORSED);
    }

    public TokenAssertion isDeclinedBy(Member... members) {
        return hasKeySignatures(
                Arrays.stream(members)
                        .map(Member::getKey)
                        .collect(toList()),
                DECLINED);
    }

    public TokenAssertion isRevokedBy(Member... members) {
        return hasKeySignatures(
                Arrays.stream(members)
                        .map(Member::getKey)
                        .collect(toList()),
                REVOKED);
    }

    public TokenAssertion hasNoSignatures() {
        Assertions.assertThat(actual.getSignaturesList()).isEmpty();
        return this;
    }

    private TokenAssertion hasKeySignatures(Collection<SecretKey> keys, @Nullable Action action) {
        List<String> members = keys.stream().map(SecretKey::getId).collect(toList());
        return hasKeySignatures(members.toArray(new String[members.size()]), action);
    }

    private TokenAssertion hasKeySignatures(String[] keyIds, @Nullable Action action) {
        List<String> signatures = actual.getSignaturesList()
                .stream()
                .filter(s -> action == null || action == s.getAction())
                .map(s -> s.getSignature().getKeyId())
                .collect(toList());
        Assertions.assertThat(signatures).contains(keyIds);
        return this;
    }
}

