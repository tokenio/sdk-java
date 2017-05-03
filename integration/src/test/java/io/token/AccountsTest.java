package io.token;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.common.TestAccount;
import io.token.common.TokenRule;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;

public class AccountsTest {
    @Rule
    public TokenRule rule = new TokenRule();

    @Test
    public void linkAccountsTest() {
        Account account = rule.account();
        assertThat(account.member().getAccounts()).containsExactly(account);

        account.member().unlinkAccounts(singletonList(account.id()));
        assertThat(account.member().getAccounts()).isEmpty();
    }

    @Test
    public void linkAccounts_relinking() {
        TestAccount account = rule.testAccount();
        assertThat(account.getAccount().member().getAccounts())
                .containsExactly(account.getAccount());

        rule.account(account.getNumber());
        rule.account(account.getNumber());
        assertThat(account.getAccount().member().getAccounts())
                .containsExactly(account.getAccount());
    }

    @Test
    public void getAccounts() {
        Account account = rule.account();
        assertThat(account.member().getAccounts()).containsExactly(account);

        List<Account> accounts = account.member().getAccounts();
        assertThat(accounts).containsExactly(account);
    }

    @Test
    public void getAccount() {
        Account account = rule.account();
        assertThat(account.member().getAccounts()).containsExactly(account);
        assertThat(account.member().getAccount(account.id())).isEqualTo(account);
    }
}
