package io.token.bank.config;

import static io.token.Destinations.swift;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.reverse;

import com.typesafe.config.Config;
import io.token.bank.TestAccount;
import io.token.bank.TestBank;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.proto.common.security.SecurityProtos;
import io.token.sdk.BankAccountAuthorizer;
import io.token.sdk.NamedAccount;

import java.time.Duration;
import java.util.List;
import java.util.Random;

/**
 * Config based implementation of the test bank. We assume that the bank we
 * are testing with has a number of bank accounts pre-configured. So all we
 * need to do is configure the account numbers on our side and use them, no
 * need to hit the bank to create accounts (which is what we do for Fank).
 */
public final class ConfigBasedTestBank extends TestBank {
    private final String bankId;
    private final List<BankAccountConfig> accounts;
    private final Random random;
    private int lastAccountIndex;

    public ConfigBasedTestBank(Config config) {
        this(new BankConfig(config));
    }

    public ConfigBasedTestBank(BankConfig config) {
        this(config.getBankId(), config.getAccounts());
    }

    public ConfigBasedTestBank(String bankId, List<BankAccountConfig> accounts) {
        this.bankId = bankId;
        this.accounts = accounts;
        this.random = new Random();
        this.lastAccountIndex = 0;
    }

    @Override
    public TestAccount nextAccount() {
        BankAccountConfig accountConfig = nextAccountConfig();
        return new TestAccount(
                accountConfig.getAccountName(),
                accountConfig.getCurrency(),
                swift(
                        accountConfig.getBic(),
                        accountConfig.getAccountNumber()).getAccount());
    }

    @Override
    public TestAccount invalidAccount() {
        BankAccountConfig accountConfig = nextAccountConfig();
        return new TestAccount(
                accountConfig.getAccountName(),
                accountConfig.getCurrency(),
                swift(
                        accountConfig.getBic(),
                        reverse(accountConfig.getAccountNumber())).getAccount());
    }

    @Override
    public BankAuthorization authorizeAccount(String username, NamedAccount account) {
        return BankAccountAuthorizer.builder(bankId)
                .useMethod(SecurityProtos.SealedMessage.MethodCase.NOOP)
                .withExpiration(Duration.ofMinutes(1))
                .build()
                        .createAuthorization(username, singletonList(account));
    }

    private BankAccountConfig nextAccountConfig() {
        int index = lastAccountIndex++ % accounts.size();
        return accounts.get(index);
    }
}
