package io.token.asserts;

import io.token.Member;
import io.token.proto.common.security.SecurityProtos;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.proto.common.transfer.TransferProtos.TransferPayload;
import io.token.security.SecretKey;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

public final class TransferAssertion extends AbstractAssert<TransferAssertion, Transfer> {
    public static TransferAssertion assertThat(Transfer transfer) {
        return new TransferAssertion(transfer);
    }

    private TransferAssertion(Transfer actual) {
        super(actual, TransferAssertion.class);
    }

    public TransferAssertion hasPayload(TransferPayload payload) {
        Assertions.assertThat(actual.getPayload()).isEqualTo(payload);
        return this;
    }

    public TransferAssertion hasAmount(double amount) {
        Assertions.assertThat(actual.getPayload().getAmount().getValue()).isEqualTo(Double.toString(amount));
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

    public TransferAssertion hasNSignatures(int count) {
        Assertions.assertThat(actual.getPayloadSignaturesCount()).isEqualTo(count);
        return this;
    }

    public TransferAssertion isSignedBy(Member... members) {
        return hasKeySignatures(Arrays.stream(members)
                .map(Member::key)
                .collect(toList()));
    }

    public TransferAssertion hasNoSignatures() {
        Assertions.assertThat(actual.getPayloadSignaturesList()).isEmpty();
        return this;
    }

    private TransferAssertion hasKeySignatures(Collection<SecretKey> keys) {
        List<String> members = keys.stream().map(SecretKey::getId).collect(toList());
        return hasKeySignatures(members.toArray(new String[members.size()]));
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

