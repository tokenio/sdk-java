package io.token.bank.fank;

import static io.token.Destinations.swift;
import static io.token.testing.sample.Sample.string;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;

import com.google.common.net.HostAndPort;
import com.typesafe.config.Config;
import io.token.bank.TestAccount;
import io.token.bank.TestBank;
import io.token.proto.bankapi.Fank;
import io.token.proto.bankapi.Fank.Client;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.sdk.NamedAccount;

public final class FankTestBank extends TestBank {
    private static final String CURRENCY = "USD";
    private final FankClient fank;
    private String bic;

    public FankTestBank(Config config) {
        this(new FankConfig(config));
    }

    public FankTestBank(FankConfig fankConfig) {
        this(fankConfig.getBic(), fankConfig.getFank(), fankConfig.useSsl());
    }

    public FankTestBank(String bic, HostAndPort fank, boolean useSsl) {
        this.bic = bic;
        this.fank = new FankClient(
                fank.getHost(),
                fank.getPort(),
                useSsl);
    }

    @Override
    public TestAccount nextAccount() {
        String accountName = "Test Account";
        String bankAccountNumber = "iban:" + randomNumeric(7);
        Fank.Client client = fank.addClient("Test " + string(), "Testoff");
        fank.addAccount(
                client,
                accountName,
                bankAccountNumber,
                1000000.00,
                CURRENCY);
        return new TestAccount(
                accountName,
                CURRENCY,
                swift(bic, bankAccountNumber).getAccount());
    }

    @Override
    public BankAuthorization authorizeAccount(String username, NamedAccount account) {
        Client client = fank.addClient("Test " + string(), "Testoff");
        fank.addAccount(
                client,
                account.getDisplayName(),
                account.getBankAccount().getSwift().getAccount(),
                1000000.00,
                CURRENCY);
        return fank.startAccountsLinking(
                username,
                client.getId(),
                singletonList(account.getBankAccount().getSwift().getAccount()));
    }
}
