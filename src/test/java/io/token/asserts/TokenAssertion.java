package io.token.asserts;

import io.token.Member;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenSignature.Action;
import io.token.security.SecretKey;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static io.token.proto.common.token.TokenProtos.TokenSignature.Action.CANCELLED;
import static io.token.proto.common.token.TokenProtos.TokenSignature.Action.ENDORSED;
import static java.util.stream.Collectors.toList;

public final class TokenAssertion extends AbstractAssert<TokenAssertion, Token> {
    public static TokenAssertion assertThat(Token token) {
        return new TokenAssertion(token);
    }

    private TokenAssertion(Token actual) {
        super(actual, TokenAssertion.class);
    }

    public TokenAssertion hasFrom(Member member) {
        Assertions
                .assertThat(actual.getPayload().getFrom().getId())
                .isEqualTo(member.memberId());
        return this;
    }

    public TokenAssertion hasRedeemerUsername(String username) {
        Assertions
                .assertThat(actual.getPayload().getTransfer().getRedeemer().getUsername())
                .isEqualTo(username);
        return this;
    }

    public TokenAssertion hasAmount(double amount) {
        Assertions
                .assertThat(actual.getPayload().getTransfer().getAmount())
                .isEqualTo(Double.toString(amount));
        return this;
    }

    public TokenAssertion hasCurrency(String currency) {
        Assertions
                .assertThat(actual.getPayload().getTransfer().getCurrency())
                .isEqualTo(currency);
        return this;
    }

    public TokenAssertion hasNSignatures(int count) {
        Assertions.assertThat(actual.getPayloadSignaturesCount()).isEqualTo(count);
        return this;
    }

    public TokenAssertion isEndorsedBy(Member... members) {
        return hasKeySignatures(
                Arrays.stream(members)
                        .map(Member::key)
                        .collect(toList()),
                ENDORSED);
    }

    public TokenAssertion isCancelledBy(Member... members) {
        return hasKeySignatures(
                Arrays.stream(members)
                        .map(Member::key)
                        .collect(toList()),
                CANCELLED);
    }

    public TokenAssertion hasNoSignatures() {
        Assertions.assertThat(actual.getPayloadSignaturesList()).isEmpty();
        return this;
    }

    private TokenAssertion hasKeySignatures(Collection<SecretKey> keys, @Nullable Action action) {
        List<String> members = keys.stream().map(SecretKey::getId).collect(toList());
        return hasKeySignatures(members.toArray(new String[members.size()]), action);
    }

    private TokenAssertion hasKeySignatures(String[] keyIds, @Nullable Action action) {
        List<String> signatures = actual.getPayloadSignaturesList()
                .stream()
                .filter(s -> action == null || action == s.getAction())
                .map(s -> s.getSignature().getKeyId())
                .collect(toList());
        Assertions.assertThat(signatures).contains(keyIds);
        return this;
    }
}

