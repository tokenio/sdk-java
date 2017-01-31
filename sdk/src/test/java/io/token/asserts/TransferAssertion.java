package io.token.asserts;

import static java.util.stream.Collectors.toList;

import io.token.Member;
import io.token.proto.common.security.SecurityProtos;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.transfer.TransferProtos.Transfer;

import java.util.Collection;
import java.util.List;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public final class TransferAssertion extends AbstractAssert<TransferAssertion, Transfer> {
    private TransferAssertion(Transfer actual) {
        super(actual, TransferAssertion.class);
    }

    public static TransferAssertion assertThat(Transfer transfer) {
        return new TransferAssertion(transfer);
    }

    public TransferAssertion hasAmount(double amount) {
        Assertions
                .assertThat(actual.getPayload().getAmount().getValue())
                .isEqualTo(Double.toString(amount));
        return this;
    }

    public TransferAssertion hasNoAmount() {
        Assertions.assertThat(actual.getPayload().hasAmount()).isFalse();
        return this;
    }

    public TransferAssertion hasCurrency(String currency) {
        Assertions.assertThat(actual.getPayload().getAmount().getCurrency()).isEqualTo(currency);
        return this;
    }

    public TransferAssertion hasDescription(String description) {
        Assertions.assertThat(actual.getPayload().getDescription()).isEqualTo(description);
        return this;
    }

    public TransferAssertion hasNSignatures(int count) {
        Assertions.assertThat(actual.getPayloadSignaturesCount()).isEqualTo(count);
        return this;
    }

    public TransferAssertion isSignedBy(Member member, Key.Level keyLevel) {
        return hasKeySignatures(member.keys()
                .stream()
                .filter(k -> k.getLevel().equals(keyLevel))
                .map(Key::getId)
                .collect(toList()));
    }

    private TransferAssertion hasKeySignatures(Collection<String> keyIds) {
        return hasKeySignatures(keyIds.toArray(new String[keyIds.size()]));
    }

    private TransferAssertion hasKeySignatures(String[] keyIds) {
        List<String> signatures = actual.getPayloadSignaturesList()
                .stream()
                .map(SecurityProtos.Signature::getKeyId)
                .collect(toList());
        Assertions.assertThat(signatures).contains(keyIds);
        return this;
    }
}

