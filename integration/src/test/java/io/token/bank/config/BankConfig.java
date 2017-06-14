/**
 * Copyright (C) 2017 Token, Inc.
 * <p>
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package io.token.bank.config;

import static java.util.stream.Collectors.toList;

import com.typesafe.config.Config;

import java.util.List;

/**
 * Parses per env test bank config.
 */
final class BankConfig {
    private final String bankId;
    private final List<BankAccountConfig> accounts;
    private final BankAccountConfig rejectAccount;

    public BankConfig(Config config) {
        this.bankId = config.getString("bank-id");
        this.accounts = config.getConfigList("bank.accounts")
                .stream()
                .map(BankAccountConfig::new)
                .collect(toList());
        this.rejectAccount = new BankAccountConfig(config.getConfig("banks.reject-account"));
    }

    public String getBankId() {
        return bankId;
    }

    public List<BankAccountConfig> getAccounts() {
        return accounts;
    }

    public BankAccountConfig getRejectAccount() {
        return rejectAccount;
    }
}
