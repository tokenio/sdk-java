package io.token.asserts;

import io.token.Account;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public final class AccountAssertion extends AbstractAssert<AccountAssertion, Account> {
    private AccountAssertion(Account actual) {
        super(actual, AccountAssertion.class);
    }

    public static AccountAssertion assertThat(Account account) {
        return new AccountAssertion(account);
    }

    public AccountAssertion hasId() {
        Assertions.assertThat(actual.id()).isNotEmpty();
        return this;
    }

    public AccountAssertion hasName(String name) {
        Assertions.assertThat(actual.name()).isEqualTo(name);
        return this;
    }

    public AccountAssertion hasBankId(String bankId) {
        Assertions.assertThat(actual.bankId()).isEqualTo(bankId);
        return this;
    }
}
