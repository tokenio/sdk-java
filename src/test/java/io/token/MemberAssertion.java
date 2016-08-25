package io.token;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import java.util.Arrays;

public final class MemberAssertion extends AbstractAssert<MemberAssertion, Member> {
    public static MemberAssertion assertThat(Member member) {
        return new MemberAssertion(member);
    }

    private MemberAssertion(Member actual) {
        super(actual, MemberAssertion.class);
    }

    public MemberAssertion hasAlias(String alias) {
        Assertions.assertThat(actual.getAliases()).contains(alias);
        return this;
    }

    public MemberAssertion hasAliases(String ... aliases) {
        Assertions.assertThat(actual.getAliases()).containsOnlyElementsOf(Arrays.asList(aliases));
        return this;
    }

    public MemberAssertion hasKey(byte[] publicKey) {
        Assertions.assertThat(actual.getPublicKeys()).contains(publicKey);
        return this;
    }

    public MemberAssertion hasKeys(byte[] ... publicKeys) {
        Assertions.assertThat(actual.getPublicKeys()).containsOnlyElementsOf(Arrays.asList(publicKeys));
        return this;
    }

    public MemberAssertion hasNKeys(int count) {
        Assertions.assertThat(actual.getPublicKeys()).hasSize(count);
        return this;
    }
}
