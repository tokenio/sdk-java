package io.token.bank;

import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.sdk.NamedAccount;

public final class TestAccount {
    private final String accountName;
    private final String currency;
    private final BankAccount bankAccount;

    public TestAccount(String accountName, String currency, BankAccount bankAccount) {
        this.accountName = accountName;
        this.currency = currency;
        this.bankAccount = bankAccount;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getCurrency() {
        return currency;
    }

    public BankAccount getBankAccount() {
        return bankAccount;
    }

    public NamedAccount toNamedAccount() {
        return new NamedAccount(bankAccount, accountName);
    }

    @Override
    public int hashCode() {
        return bankAccount.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TestAccount)) {
            return false;
        }

        return bankAccount.equals(((TestAccount) obj).bankAccount);
    }
}
