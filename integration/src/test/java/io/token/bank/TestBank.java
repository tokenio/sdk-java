package io.token.bank;

import com.typesafe.config.Config;
import io.token.bank.config.ConfigBasedTestBank;
import io.token.bank.fank.FankTestBank;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.sdk.NamedAccount;

/**
 * Abstracts away a test bank backend.
 */
public abstract class TestBank {
    /**
     * Creates {@link TestBank} implementation based on the config.
     *
     * @param config test configuration
     * @return test bank instance
     */
    public static TestBank create(Config config) {
        if (config.hasPath("fank")) {
            return new FankTestBank(config);
        } else if (config.hasPath("bank")) {
            return new ConfigBasedTestBank(config);
        }
        throw new IllegalStateException("Not supported configuration");
    }

    /**
     * Returns a random account that can be linked.
     *
     * @return random account
     */
    public abstract NamedAccount randomAccount();

    /**
     * Looks up an account given the account number.
     *
     * @param accountNumber account number
     * @return looked up account
     */
    public abstract NamedAccount lookupAccount(String accountNumber);

    /**
     * Produces bank authorization for the given account.
     *
     * @param username username to authorize
     * @param account account to authorize access to
     * @return bank authorization
     */
    public abstract BankAuthorization authorizeAccount(String username, NamedAccount account);
}
