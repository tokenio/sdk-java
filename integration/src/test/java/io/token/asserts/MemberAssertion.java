/**
 * Copyright (C) 2017 Token, Inc.
 * <p>
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package io.token.asserts;

import io.token.Member;
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

    public MemberAssertion hasUsername(String username) {
        Assertions.assertThat(actual.usernames()).contains(username);
        return this;
    }

    public MemberAssertion hasUsernames(String... usernames) {
        return hasUsernames(Arrays.asList(usernames));
    }

    public MemberAssertion hasUsernames(Collection<String> usernames) {
        Assertions.assertThat(actual.usernames()).containsOnlyElementsOf(usernames);
        return this;
    }

    public MemberAssertion hasOneUsername() {
        return hasNUsernames(1);
    }

    public MemberAssertion hasNUsernames(int count) {
        Assertions.assertThat(actual.usernames()).hasSize(count);
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
