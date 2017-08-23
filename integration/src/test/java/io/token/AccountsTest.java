package io.token;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.common.LinkedAccount;
import io.token.common.TokenRule;
import io.token.proto.common.bank.BankProtos.Bank;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;

public class AccountsTest {
    @Rule
    public TokenRule rule = new TokenRule();

    @Test
    public void linkAccounts() {
        LinkedAccount account = rule.linkedAccount();
        assertThat(account
                .getMember()
                .getAccounts()).containsExactly(account.getAccount());

        account.getMember().unlinkAccounts(singletonList(account.getId()));
        assertThat(account.getMember().getAccounts()).isEmpty();
    }

    @Test
    public void linkAccounts_relinking() {
        LinkedAccount account = rule.linkedAccount();
        assertThat(account
                .getMember()
                .getAccounts()).containsExactly(account.getAccount());

        rule.relinkAccount(account);
        rule.relinkAccount(account);
        assertThat(account
                .getMember()
                .getAccounts()).containsExactly(account.getAccount());
    }

    @Test
    public void getAccounts() {
        LinkedAccount account = rule.linkedAccount();
        assertThat(account
                .getMember()
                .getAccounts()).containsExactly(account.getAccount());

        List<Account> accounts = account.getMember().getAccounts();
        assertThat(accounts).containsExactly(account.getAccount());
    }

    @Test
    public void getAccountsMultiple() {
        LinkedAccount account1 = rule.linkedAccount();
        LinkedAccount account2 = rule.linkedAccount(account1.getMember());

        List<Account> accounts = account1.getMember().getAccounts();
        assertThat(accounts).contains(account1.getAccount());
        assertThat(accounts).contains(account2.getAccount());
    }

    @Test
    public void getAccount() {
        LinkedAccount account = rule.linkedAccount();
        assertThat(account
                .getMember()
                .getAccounts()).containsExactly(account.getAccount());
        assertThat(account.getMember().getAccount(account.getId())).isEqualTo(
                account.getAccount());
    }

    @Test
    public void getBalance() {
        LinkedAccount account = rule.linkedAccount();
        assertThat(account.getBalance()).isNotNaN();
    }


    @Test
    public void setDefaultBank() {
        String bankId = rule
                .linkedAccount()
                .getMember()
                .getBanks()
                .get(0)
                .getId();
        rule.linkedAccount().getMember().setDefaultBank(bankId);
    }

    @Test
    public void getDefaultBank() {
        List<String> bankIds = rule
                .linkedAccount()
                .getMember()
                .getBanks()
                .stream()
                .map(Bank::getId)
                .collect(Collectors.toList());
        assertThat(rule.linkedAccount().getMember().getDefaultBank()).isIn(
                bankIds);
    }
}
