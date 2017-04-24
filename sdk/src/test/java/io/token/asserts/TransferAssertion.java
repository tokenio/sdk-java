/**
 * Copyright (C) 2017 Token, Inc.
 * <p>
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package io.token.asserts;

import io.token.Member;
import io.token.proto.common.security.SecurityProtos;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.transaction.TransactionProtos.TransactionStatus;
import io.token.proto.common.transfer.TransferProtos.Transfer;

import java.util.Collection;
import java.util.LinkedList;
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

    public TransferAssertion isSuccessful() {
        return hasStatus(TransactionStatus.SUCCESS);
    }

    public TransferAssertion hasStatus(TransactionStatus status) {
        Assertions
                .assertThat(actual.getStatus())
                .isEqualTo(status);
        return this;
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
        List<String> keyIds = new LinkedList<>();
        for (Key key : member.keys()) {
            if (key.getLevel().equals(keyLevel)) {
                keyIds.add(key.getId());
            }
        }
        return hasKeySignatures(keyIds);
    }

    private TransferAssertion hasKeySignatures(Collection<String> keyIds) {
        return hasKeySignatures(keyIds.toArray(new String[keyIds.size()]));
    }

    private TransferAssertion hasKeySignatures(String[] keyIds) {
        List<String> signatures = new LinkedList<>();
        for (SecurityProtos.Signature signature : actual.getPayloadSignaturesList()) {
            signatures.add(signature.getKeyId());
        }
        Assertions.assertThat(signatures).contains(keyIds);
        return this;
    }
}

