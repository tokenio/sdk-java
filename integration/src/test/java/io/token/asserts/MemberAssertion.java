/**
 * Copyright (C) 2017 Token, Inc.
 * <p>
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package io.token.asserts;

import io.token.Member;
import io.token.proto.common.alias.AliasProtos;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.security.SecurityProtos.Key;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public final class MemberAssertion extends AbstractAssert<MemberAssertion, Member> {
    private MemberAssertion(Member actual) {
        super(actual, MemberAssertion.class);
    }

    public static MemberAssertion assertThat(Member member) {
        return new MemberAssertion(member);
    }

    public MemberAssertion hasAlias(Alias alias) {
        Assertions.assertThat(actual.aliases()).contains(alias);
        return this;
    }

    public MemberAssertion hasAliases(Alias... aliases) {
        return hasAliases(Arrays.asList(aliases));
    }

    public MemberAssertion hasAliases(Collection<Alias> aliases) {
        Assertions.assertThat(actual.aliases()).containsOnlyElementsOf(aliases);
        return this;
    }

    public MemberAssertion hasOneAlias() {
        return hasNAliases(1);
    }

    public MemberAssertion hasNAliases(int count) {
        Assertions.assertThat(actual.aliases()).hasSize(count);
        return this;
    }

    public MemberAssertion hasKey(Key key) {
        Assertions.assertThat(actual.keys()).contains(key);
        return this;
    }

    public MemberAssertion hasKey(String keyId) {
        List<String> keyIds = new LinkedList<String>();
        for (Key key : actual.keys()) {
            keyIds.add(key.getId());
        }
        Assertions.assertThat(keyIds).contains(keyId);
        return this;
    }

    public MemberAssertion hasOneKey() {
        return hasNKeys(1);
    }

    public MemberAssertion hasNKeys(int count) {
        Assertions.assertThat(actual.keys()).hasSize(count);
        return this;
    }
}
