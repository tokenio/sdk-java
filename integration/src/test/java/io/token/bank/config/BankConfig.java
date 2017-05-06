/**
 * Copyright (C) 2017 Token, Inc.
 * <p>
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package io.token.bank.config;

import com.typesafe.config.Config;

import java.util.List;

/**
 * Parses per env test bank config.
 */
public class BankConfig {
    private final Config config;

    public BankConfig(Config config) {
        this.config = config;
    }

    public String getBankId() {
        return config.getString("bank-id");
    }

    public String getBic() {
        return config.getString("bank-bic");
    }

    public List<String> getAccounts() {
        return config.getStringList("bank.accounts");
    }
}
