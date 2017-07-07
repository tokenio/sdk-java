/**
 * Copyright (C) 2017 Token, Inc.
 * <p>
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package io.token.asserts;

import static io.token.proto.common.token.TokenProtos.TokenSignature.Action.CANCELLED;
import static io.token.proto.common.token.TokenProtos.TokenSignature.Action.ENDORSED;

import io.token.Member;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenSignature.Action;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public final class TokenAssertion extends AbstractAssert<TokenAssertion, Token> {
    private TokenAssertion(Token actual) {
        super(actual, TokenAssertion.class);
    }

    public static TokenAssertion assertThat(Token token) {
        return new TokenAssertion(token);
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

    public TokenAssertion hasDescription(String description) {
        Assertions
                .assertThat(actual.getPayload().getDescription())
                .isEqualTo(description);
        return this;
    }

    public TokenAssertion hasAmount(double amount) {
        Assertions
                .assertThat(actual.getPayload().getTransfer().getLifetimeAmount())
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

    public TokenAssertion isEndorsedBy(Member member, Key.Level keyLevel) {
        List<String> keyIdList = getKeysForLevel(member, keyLevel);
        return hasKeySignatures(keyIdList, ENDORSED);
    }

    public TokenAssertion isCancelledBy(Member member, Key.Level keyLevel) {
        List<String> keyIdList = getKeysForLevel(member, keyLevel);
        return hasKeySignatures(keyIdList, CANCELLED);
    }

    public TokenAssertion hasNoSignatures() {
        Assertions.assertThat(actual.getPayloadSignaturesList()).isEmpty();
        return this;
    }

    private TokenAssertion hasKeySignatures(Collection<String> keyIds, @Nullable Action action) {
        return hasKeySignatures(keyIds.toArray(new String[keyIds.size()]), action);
    }

    private TokenAssertion hasKeySignatures(String[] keyIds, @Nullable Action action) {
        List<String> keyIdList = new LinkedList<>();
        for (TokenProtos.TokenSignature signature : actual.getPayloadSignaturesList()) {
            if (action == null || action == signature.getAction()) {
                keyIdList.add(signature.getSignature().getKeyId());
            }
        }
        Assertions.assertThat(keyIdList).contains(keyIds);
        return this;
    }

    private static List<String> getKeysForLevel(Member member, Key.Level keyLevel) {
        List<String> list = new LinkedList<>();
        for (Key key : member.keys()) {
            if (key.getLevel().equals(keyLevel)) {
                list.add(key.getId());
            }
        }
        return list;
    }
}

