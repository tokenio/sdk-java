package io.token.common;

import io.token.Account;

public final class TestAccount {
    private final Account account;
    private final String number;

    public TestAccount(String number, Account account) {
        this.account = account;
        this.number = number;
    }

    public String getNumber() {
        return number;
    }

    public Account getAccount() {
        return account;
    }
}
