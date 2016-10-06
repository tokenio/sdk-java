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

    public MemberAssertion hasAlias(String alias) {
        Assertions.assertThat(actual.aliases()).contains(alias);
        return this;
    }

    public MemberAssertion hasAliases(String ... aliases) {
        return hasAliases(Arrays.asList(aliases));
    }

    public MemberAssertion hasAliases(Collection<String> aliases) {
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

    public MemberAssertion hasKey(byte[] publicKey) {
        Assertions.assertThat(actual.publicKeys()).contains(publicKey);
        return this;
    }

    public MemberAssertion hasKeys(byte[] ... publicKeys) {
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
