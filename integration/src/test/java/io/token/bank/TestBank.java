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
     * Returns a next account that can be linked.
     *
     * @return next account
     */
    public abstract TestAccount nextAccount();

    /**
     * Returns an invalid account that can be linked.
     *
     * @return invalid account
     */
    public abstract TestAccount invalidAccount();


    /**
     * Returns a valid but 'reject' account, transfers to/from that account
     * always rejected.
     *
     * @return reject account
     */
    public abstract TestAccount rejectAccount();

    /**
     * Produces bank authorization for the given account.
     *
     * @param username username to authorize
     * @param account account to authorize access to
     * @return bank authorization
     */
    public abstract BankAuthorization authorizeAccount(String username, NamedAccount account);
}
