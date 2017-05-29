package io.token.bank.config;

final class BankAccountConfig {
    private final String accountName;
    private final String bic;
    private final String accountNumber;
    private final String currency;

    public BankAccountConfig(
            String accountName,
            String bic,
            String accountNumber,
            String currency) {
        this.accountName = accountName;
        this.bic = bic;
        this.accountNumber = accountNumber;
        this.currency = currency;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getBic() {
        return bic;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getCurrency() {
        return currency;
    }
}
