package io.token.bank.config;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;

import com.typesafe.config.Config;
import io.token.bank.TestBank;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.proto.common.security.SecurityProtos;
import io.token.sdk.BankAccount;
import io.token.sdk.BankAccountAuthorizer;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import javax.xml.datatype.DatatypeFactory;

/**
 * Config based implementation of the test bank. We assume that the bank we
 * are testing with has a number of bank accounts pre-configured. So all we
 * need to do is configure the account numbers on our side and use them, no
 * need to hit the bank to create accounts (which is what we do for Fank).
 */
public final class ConfigBasedTestBank extends TestBank {
    private final String bankId;
    private final String bic;
    private final List<String> accounts;
    private final Random random;

    public ConfigBasedTestBank(Config config) {
        this(new BankConfig(config));
    }

    public ConfigBasedTestBank(BankConfig config) {
        this(config.getBankId(), config.getBic(), config.getAccounts());
    }

    public ConfigBasedTestBank(String bankId, String bic, List<String> accounts) {
        this.bankId = bankId;
        this.bic = bic;
        this.accounts = accounts;
        this.random = new Random();
    }

    @Override
    public BankAccount randomAccount() {
        int index = random.nextInt(accounts.size());
        String accountNumber = accounts.get(index);
        return new BankAccount(bic, accountNumber, accountNumber);
    }

    @Override
    public BankAccount lookupAccount(String accountNumber) {
        checkState(accounts.contains(accountNumber));
        return new BankAccount(bic, accountNumber, accountNumber);
    }

    @Override
    public BankAuthorization authorizeAccount(String username, BankAccount account) {
        return BankAccountAuthorizer.builder(bankId)
                .useMethod(SecurityProtos.SealedMessage.MethodCase.NOOP)
                .withExpiration(Duration.ofMinutes(1))
                .build()
                        .createAuthorization(username, singletonList(account));
    }
}
