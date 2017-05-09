package io.token.bank.fank;

import static io.token.testing.sample.Sample.string;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;

import com.google.common.net.HostAndPort;
import com.typesafe.config.Config;
import io.token.bank.TestBank;
import io.token.proto.bankapi.Fank;
import io.token.proto.bankapi.Fank.Client;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.sdk.BankAccount;

public final class FankTestBank extends TestBank {
    private static final String BIC = "IRONUSCA000";
    private final FankClient fank;

    public FankTestBank(Config config) {
        this(new FankConfig(config));
    }

    public FankTestBank(FankConfig fankConfig) {
        this(fankConfig.getFank(), fankConfig.useSsl());
    }

    public FankTestBank(HostAndPort fank, boolean useSsl) {
        this.fank = new FankClient(
                fank.getHost(),
                fank.getPort(),
                useSsl);
    }

    @Override
    public BankAccount randomAccount() {
        String bankAccountNumber = "iban:" + randomNumeric(7);
        Fank.Client client = fank.addClient("Test " + string(), "Testoff");
        fank.addAccount(
                client,
                "Test Account",
                bankAccountNumber,
                1000000.00,
                "USD");
        return new BankAccount(BIC, bankAccountNumber, "Test Account");
    }

    @Override
    public BankAccount lookupAccount(String accountNumber) {
        return new BankAccount(BIC, accountNumber, "Test Account");
    }

    @Override
    public BankAuthorization authorizeAccount(String username, BankAccount account) {
        Client client = fank.addClient("Test " + string(), "Testoff");
        fank.addAccount(
                client,
                account.getDisplayName(),
                account.getAccountNumber(),
                1000000.00,
                "USD");
        return fank.startAccountsLinking(
                username,
                client.getId(),
                singletonList(account.getAccountNumber()));
    }
}
