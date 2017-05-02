package io.token.bank;

import com.typesafe.config.Config;
import io.token.bank.fank.FankTestBank;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.sdk.BankAccount;

public abstract class TestBank {
    public static TestBank create(Config config) {
        if (config.hasPath("fank")) {
            return new FankTestBank(config);
        } else {
            throw new IllegalStateException("Not supported configuration");
        }
    }

    public abstract BankAccount randomAccount();
    public abstract BankAccount lookupAccount(String accountNumber);
    public abstract BankAuthorization authorizeAccount(String username, BankAccount account);
}
