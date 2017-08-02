package io.token.bank.config;

import static io.token.Destinations.swift;
import static java.util.Collections.singletonList;

import com.typesafe.config.Config;
import io.token.bank.TestAccount;
import io.token.bank.TestBank;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.proto.common.security.SecurityProtos;
import io.token.sdk.BankAccountAuthorizer;
import io.token.sdk.NamedAccount;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Config based implementation of the test bank. We assume that the bank we
 * are testing with has a number of bank accounts pre-configured. So all we
 * need to do is configure the account numbers on our side and use them, no
 * need to hit the bank to create accounts (which is what we do for Fank).
 */
public final class ConfigBasedTestBank extends TestBank {
    private final String bankId;
    private final List<BankAccountConfig> accounts;
    private final BankAccountConfig rejectAccount;
    private int lastAccountIndex;

    public ConfigBasedTestBank(Config config) {
        this(new BankConfig(config));
    }

    public ConfigBasedTestBank(BankConfig config) {
        this(config.getBankId(), config.getAccounts(), config.getRejectAccount());
    }

    public ConfigBasedTestBank(
            String bankId,
            List<BankAccountConfig> accounts,
            BankAccountConfig rejectAccount) {
        this.bankId = bankId;
        this.accounts = accounts;
        this.rejectAccount = rejectAccount;
        this.lastAccountIndex = 0;
    }

    @Override
    public TestAccount nextAccount(Optional<TestAccount> counterParty) {
        return findNextAccount(counterParty);
    }

    @Override
    public TestAccount invalidAccount() {
        TestAccount nextAccount = findNextAccount(Optional.empty());
        return new TestAccount(
                nextAccount.getAccountName(),
                nextAccount.getCurrency(),
                swift(
                        nextAccount.getBankAccount().getSwift().getBic(),
                        "9999999999").getAccount());
    }

    @Override
    public TestAccount rejectAccount() {
        BankAccountConfig accountConfig = rejectAccount;
        return new TestAccount(
                accountConfig.getAccountName(),
                accountConfig.getCurrency(),
                swift(
                        accountConfig.getBic(),
                        accountConfig.getAccountNumber()).getAccount());
    }

    @Override
    public BankAuthorization authorizeAccount(String alias, NamedAccount account) {
        return BankAccountAuthorizer.builder(bankId)
                .useMethod(SecurityProtos.SealedMessage.MethodCase.NOOP)
                .withExpiration(Duration.ofMinutes(1))
                .build()
                        .createAuthorization(alias, singletonList(account));
    }

    private TestAccount findNextAccount(Optional<TestAccount> counterParty) {
        while (true) {
            int index = lastAccountIndex++ % accounts.size();
            BankAccountConfig nextConfig = accounts.get(index);
            TestAccount nextAccount = new TestAccount(
                    nextConfig.getAccountName(),
                    nextConfig.getCurrency(),
                    swift(
                            nextConfig.getBic(),
                            nextConfig.getAccountNumber()).getAccount());

            // Check if the nextAccount is different from counter party account.
            boolean found = counterParty
                    .map(cp -> !cp.equals(nextAccount))
                    .orElse(true);

            if (found) {
                return nextAccount;
            }
        }
    }
}
