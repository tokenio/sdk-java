package io.token.asserts;

import io.token.Account;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public final class AccountAssertion extends AbstractAssert<AccountAssertion, Account> {
    public static AccountAssertion assertThat(Account account) {
        return new AccountAssertion(account);
    }

    private AccountAssertion(Account actual) {
        super(actual, AccountAssertion.class);
    }

    public AccountAssertion hasId() {
        Assertions.assertThat(actual.getId()).isNotEmpty();
        return this;
    }

    public AccountAssertion hasName(String name) {
        Assertions.assertThat(actual.getName()).isEqualTo(name);
        return this;
    }
}
