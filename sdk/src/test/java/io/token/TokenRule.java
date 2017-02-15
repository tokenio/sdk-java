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

import com.google.common.net.HostAndPort;
import io.token.proto.bankapi.Fank;
import io.token.proto.common.security.SecurityProtos.SealedMessage;
import io.token.util.Util;

import java.time.Duration;
import java.util.List;

import org.junit.rules.ExternalResource;

/**
 * One can control what gateway the tests run against by setting system property on the
 * command line. E.g:
 * <p>
 * ./gradlew -DTOKEN_GATEWAY=some-ip -DTOKEN_BANK=some-ip test
 */
public class TokenRule extends ExternalResource {
    public static final String DEFAULT_BANK_ID = "iron";
    private final boolean useSsl;
    private final Token token;
    private final BankClient bankClient;

    public TokenRule() {
        useSsl = Boolean.parseBoolean(getEnvProperty("TOKEN_USE_SSL", "false"));

        HostAndPort bank = HostAndPort
                .fromString(getEnvProperty("TOKEN_BANK", "localhost"))
                .withDefaultPort(9100);
        this.bankClient = new BankClient(
                bank.getHostText(),
                bank.getPort(),
                useSsl);
        this.token = newSdkInstance();
    }

    @Override
    protected void after() {
        bankClient.close();
        token.close();
    }

    public Token newSdkInstance() {
        HostAndPort gateway = HostAndPort
                .fromString(getEnvProperty("TOKEN_GATEWAY", "localhost"))
                .withDefaultPort(9000);

        return Token.builder()
                .hostName(gateway.getHostText())
                .port(gateway.getPort())
                .timeout(Duration.ofMinutes(10))  // Set high for easy debugging.
                .build();
    }

    public Member member() {
        String username = "username-" + Util.generateNonce();
        return token.createMember(username);
    }

    public Account account() {
        Member member = member();
        String bankAccountNumber = "iban:" + randomInt(7);
        Fank.Client client = bankClient.addClient("Test " + string(), "Testoff");
        bankClient.addAccount(client, "Test Account", bankAccountNumber, 1000000.00, "USD");
        List<SealedMessage> accountLinkPayloads = bankClient.startAccountsLinking(
                member.firstUsername(),
                client.getId(),
                singletonList(bankAccountNumber));

        return member
                .linkAccounts(DEFAULT_BANK_ID, accountLinkPayloads)
                .get(0);
    }

    public Token token() {
        return token;
    }

    public BankClient bankClient() {
        return bankClient;
    }

    private static String getEnvProperty(String name, String defaultValue) {
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
