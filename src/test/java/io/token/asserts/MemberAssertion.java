package io.token.asserts;

import io.token.Member;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import java.util.Arrays;
import java.util.Collection;

public final class MemberAssertion extends AbstractAssert<MemberAssertion, Member> {
    public static MemberAssertion assertThat(Member member) {
        return new MemberAssertion(member);
    }

    private MemberAssertion(Member actual) {
        super(actual, MemberAssertion.class);
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

    public MemberAssertion hasKey(byte[] publicKey) {
        Assertions.assertThat(actual.publicKeys()).contains(publicKey);
        return this;
    }

    public MemberAssertion hasKeys(byte[]... publicKeys) {
        Assertions.assertThat(actual.publicKeys()).containsOnlyElementsOf(Arrays.asList(publicKeys));
        return this;
    }

    public MemberAssertion hasOneKey() {
        return hasNKeys(1);
    }

    public MemberAssertion hasNKeys(int count) {
        Assertions.assertThat(actual.publicKeys()).hasSize(count);
        return this;
    }
}
