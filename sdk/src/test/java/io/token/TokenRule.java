/**
 * Copyright (C) 2017 Token, Inc.
 * <p>
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package io.token;

import static java.lang.Math.pow;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.util.Strings.isNullOrEmpty;

import io.token.proto.bankapi.Fank;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.sdk.BankAccount;
import io.token.util.Util;

import org.junit.rules.ExternalResource;

/**
 * One can control what gateway the tests run against by setting system property on the
 * command line. E.g:
 * <p>
 * ./gradlew -DTOKEN_ENV=development test
 */
public class TokenRule extends ExternalResource {
    private final EnvConfig config;
    private final TokenIO tokenIO;
    private final BankClient bankClient;

    public TokenRule() {
        this.config = new EnvConfig(getEnvProperty("TOKEN_ENV", "local"));
        this.bankClient = new BankClient(
                config.getFank().getHost(),
                config.getFank().getPort(),
                config.useSsl());
        this.tokenIO = newSdkInstance();
    }

    @Override
    protected void after() {
        tokenIO.close();
    }

    public String getBankId() {
        return config.getBankId();
    }

    public TokenIO newSdkInstance() {
        return TokenIO.builder()
                .hostName(config.getGateway().getHost())
                .port(config.getGateway().getPort())
                .timeout(10 * 60 * 1_000)  // Set high for easy debugging.
                .build();
    }

    public Member member() {
        String username = "username-" + Util.generateNonce();
        return tokenIO.createMember(username);
    }

    public Account account() {
        Member member = member();
        String bankAccountNumber = "iban:" + randomInt(7);
        Fank.Client client = bankClient.addClient("Test " + string(), "Testoff");
        bankClient.addAccount(client, "Test Account", bankAccountNumber, 1000000.00, "USD");
        BankAuthorization authorization = bankClient.startAccountsLinking(
                member.firstUsername(),
                client.getId(),
                singletonList(bankAccountNumber));

        return member
                .linkAccounts(authorization)
                .get(0);
    }

    public BankAccount unlinkedAccount() {
        String bankAccountNumber = "iban:" + randomInt(7);
        Fank.Client client = bankClient.addClient("Test " + string(), "Testoff");
        bankClient.addAccount(client, "Test Account", bankAccountNumber, 1000000.00, "USD");
        return new BankAccount(bankAccountNumber, "Test Account");
    }

    public TokenIO token() {
        return tokenIO;
    }

    public BankClient bankClient() {
        return bankClient;
    }

    public static String getEnvProperty(String name, String defaultValue) {
        String override = System.getenv(name);
        if (!isNullOrEmpty(override)) {
            return override;
        }

        override = System.getProperty(name);
        if (!isNullOrEmpty(override)) {
            return override;
        }

        return defaultValue;
    }

    private static String string() {
        int length = randomInt(3, 7);
        return randomAlphabetic(length);
    }

    private static int randomInt(int digits) {
        return randomInt(
                (int) pow(10, digits),
                (int) pow(10, digits + 1) - 1);
    }

    private static int randomInt(int min, int max) {
        return (int) (Math.random() * (max - min)) + min;
    }
}
