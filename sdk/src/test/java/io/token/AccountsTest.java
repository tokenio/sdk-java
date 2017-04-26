package io.token;

import static io.token.TokenRule.DEFAULT_BANK_ID;
import static io.token.testing.sample.Sample.integer;
import static io.token.testing.sample.Sample.string;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.asserts.AccountAssertion;
import io.token.proto.bankapi.Fank;
import io.token.proto.banklink.Banklink;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.proto.common.security.SecurityProtos.SealedMessage;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;

public class AccountsTest {
    @Rule
    public TokenRule rule = new TokenRule();

    private final int randomized = integer(1000000, 9999999);
    private final BankClient fank = rule.bankClient();
    private final Fank.Client client = fank.addClient(string(), string());
    private final Member member = rule.member();

    @Test
    public void linkAccountsTest() {
        List<Account> accounts = linkAccounts();

        List<String> accountIdList = new LinkedList<>();
        for (Account account : accounts) {
            accountIdList.add(account.id());
        }

        member.unlinkAccounts(accountIdList);
        assertThat(member.getAccounts()).isEmpty();
    }

    @Test
    public void linkAccounts_relinking() {
        linkAccounts();
        linkAccounts();
        linkAccounts();

        assertThat(member.getAccounts()).hasSize(2);
    }

    @Test
    public void getAccounts() {
        linkAccounts();

        List<Account> accounts = member.getAccounts();
        sortedAccounts(accounts);

        assertThat(accounts).hasSize(2);
        AccountAssertion.assertThat(accounts.get(0))
                .hasId()
                .hasBankId(DEFAULT_BANK_ID)
                .hasName("Checking");
        AccountAssertion.assertThat(accounts.get(1))
                .hasId()
                .hasBankId(DEFAULT_BANK_ID)
                .hasName("Saving");
    }


    @Test
    public void getAccount() {
        linkAccounts();

        List<Account> accounts = member.getAccounts();
        sortedAccounts(accounts);
        assertThat(accounts).hasSize(2);

        Account account = member.getAccount(accounts.get(0).id());
        AccountAssertion.assertThat(account)
                .hasId()
                .hasName("Checking");
    }

    private List<Account> linkAccounts() {
        Fank.Account checking = fank.addAccount(
                client,
                "Checking",
                "iban:checking-" + randomized,
                1000000.0,
                "USD");

        Fank.Account saving = fank.addAccount(
                client,
                "Saving",
                "iban:saving-" + randomized,
                1000000.0,
                "USD");

        BankAuthorization authorization = fank.startAccountsLinking(
                member.firstUsername(),
                client.getId(),
                Arrays.asList(checking.getAccountNumber(), saving.getAccountNumber()));

        List<Account> accounts = link(authorization);

        assertThat(accounts).hasSize(2);
        AccountAssertion.assertThat(accounts.get(0))
                .hasId()
                .hasName("Checking");
        AccountAssertion.assertThat(accounts.get(1))
                .hasId()
                .hasName("Saving");

        return accounts;
    }

    private List<Account> link(BankAuthorization authorization) {
        List<Account> accounts = member.linkAccounts(authorization);
        sortedAccounts(accounts);
        return accounts;
    }

    private void sortedAccounts(List<Account> accounts) {
        Collections.sort(accounts, new Comparator<Account>() {
            public int compare(Account thisAccount, Account otherAccount) {
                return thisAccount.name().compareTo(otherAccount.name());
            }
        });
    }
}
