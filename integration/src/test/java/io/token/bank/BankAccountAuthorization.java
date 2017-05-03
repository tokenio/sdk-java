package io.token.bank;

import io.token.proto.banklink.Banklink.BankAuthorization;

public final class BankAccountAuthorization {
    private final String accountNumber;
    private final BankAuthorization authorization;

    public BankAccountAuthorization(
            String accountNumber,
            BankAuthorization authorization) {
        this.accountNumber = accountNumber;
        this.authorization = authorization;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BankAuthorization getAuthorization() {
        return authorization;
    }
}
