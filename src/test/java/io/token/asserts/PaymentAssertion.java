package io.token.asserts;

import io.token.Member;
import io.token.proto.common.payment.PaymentProtos.Payment;
import io.token.proto.common.payment.PaymentProtos.PaymentPayload;
import io.token.proto.common.security.SecurityProtos;
import io.token.security.SecretKey;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

public final class PaymentAssertion extends AbstractAssert<PaymentAssertion, Payment> {
    public static PaymentAssertion assertThat(Payment payment) {
        return new PaymentAssertion(payment);
    }

    private PaymentAssertion(Payment actual) {
        super(actual, PaymentAssertion.class);
    }

    public PaymentAssertion hasPayload(PaymentPayload payload) {
        Assertions.assertThat(actual.getPayload()).isEqualTo(payload);
        return this;
    }

    public PaymentAssertion hasAmount(double amount) {
        Assertions.assertThat(actual.getPayload().getAmount().getValue()).isEqualTo(Double.toString(amount));
        return this;
    }

    public PaymentAssertion hasCurrency(String currency) {
        Assertions.assertThat(actual.getPayload().getAmount().getCurrency()).isEqualTo(currency);
        return this;
    }

    public PaymentAssertion hasNSignatures(int count) {
        Assertions.assertThat(actual.getSignatureCount()).isEqualTo(count);
        return this;
    }

    public PaymentAssertion isSignedBy(Member... members) {
        return hasKeySignatures(Arrays.stream(members)
                        .map(Member::key)
                        .collect(toList()));
    }

    public PaymentAssertion hasNoSignatures() {
        Assertions.assertThat(actual.getSignatureList()).isEmpty();
        return this;
    }

    private PaymentAssertion hasKeySignatures(Collection<SecretKey> keys) {
        List<String> members = keys.stream().map(SecretKey::getId).collect(toList());
        return hasKeySignatures(members.toArray(new String[members.size()]));
    }

    private PaymentAssertion hasKeySignatures(String[] keyIds) {
        List<String> signatures = actual.getSignatureList()
                .stream()
                .map(SecurityProtos.Signature::getKeyId)
                .collect(toList());
        Assertions.assertThat(signatures).contains(keyIds);
        return this;
    }
}

