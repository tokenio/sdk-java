/**
 * Copyright (C) 2017 Token, Inc.
 * <p>
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package io.token;

import static org.assertj.core.util.Strings.isNullOrEmpty;
import static org.junit.Assume.assumeFalse;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.token.bank.TestBank;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.sdk.BankAccount;
import io.token.util.Util;

import java.util.regex.Pattern;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 * One can control what gateway the tests run against by setting system property on the
 * command line. E.g:
 * <p>
 * ./gradlew -DTOKEN_ENV=development test
 */
public class TokenRule implements MethodRule {
    private final EnvConfig config;
    private final TokenIO tokenIO;
    private final TestBank testBank;

    public TokenRule() {
        Config config = ConfigFactory.load(getEnvProperty("TOKEN_ENV", "local"));

        this.config = new EnvConfig(config);
        this.testBank = TestBank.create(config);
        this.tokenIO = newSdkInstance();
    }

    @Override
    public Statement apply(final Statement base, final FrameworkMethod method, Object target) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                for (Pattern p : config.getBlackList()) {
                    String methodName = method.getDeclaringClass().getName()
                            + "."
                            + method.getName();
                    assumeFalse(
                            "Skipping blacklisted test: " + methodName,
                            p.matcher(methodName).matches());
                }
                try {
                    base.evaluate();
                } finally {
                    tokenIO.close();
                }
            }
        };
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
        return testAccount().getAccount();
    }

    public Account account(String accountNumber) {
        return testAccount(accountNumber).getAccount();
    }

    public TestAccount testAccount() {
        Member member = member();
        BankAccount account = testBank.randomAccount();
        BankAuthorization auth = testBank.authorizeAccount(member.firstUsername(), account);
        return new TestAccount(
                account.getIdentifier(),
                member
                        .linkAccounts(auth)
                        .get(0));
    }

    public TestAccount testAccount(String accountNumber) {
        Member member = member();
        BankAccount account = testBank.lookupAccount(accountNumber);
        BankAuthorization auth = testBank.authorizeAccount(member.firstUsername(), account);
        return new TestAccount(
                accountNumber,
                member
                        .linkAccounts(auth)
                        .get(0));
    }

    public BankAccount unlinkedAccount() {
        return testBank.randomAccount();
    }

    public TokenIO token() {
        return tokenIO;
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
}
