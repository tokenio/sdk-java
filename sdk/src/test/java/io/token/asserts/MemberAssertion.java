package io.token.asserts;

import static java.util.stream.Collectors.toList;

import io.token.Member;
import io.token.proto.common.security.SecurityProtos.Key;

import java.util.Arrays;
import java.util.Collection;
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
        Assertions.assertThat(actual.keys()
                .stream()
                .map(Key::getId)
                .collect(toList())).contains(keyId);
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
