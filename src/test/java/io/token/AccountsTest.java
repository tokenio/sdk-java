package io.token;

import io.token.asserts.AccountAssertion;
import io.token.proto.bankapi.Fank;
import io.token.proto.common.security.SecurityProtos.SealedMessage;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static io.token.testing.sample.Sample.integer;
import static io.token.testing.sample.Sample.string;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class AccountsTest {
    @Rule
    public TokenRule rule = new TokenRule();
    private final Member member = rule.member();
    private final int randomized = integer(1000000, 9999999);
    private final BankClient fank = rule.bankClient();
    private final Fank.Client client = fank.addClient(string(), string());

    @Test
    public void linkAccounts() {
        String bankId = "iron";

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

        List<SealedMessage> payloads = fank.startAccountsLinking(
                member.firstUsername(),
                client.getId(),
                Arrays.asList(checking.getAccountNumber(), saving.getAccountNumber()));

        List<Account> accounts = link(bankId, payloads);

        assertThat(accounts).hasSize(2);
        AccountAssertion.assertThat(accounts.get(0))
                .hasId()
                .hasName("Checking");
        AccountAssertion.assertThat(accounts.get(1))
                .hasId()
                .hasName("Saving");
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

        List<Account> accounts = member.getAccounts()
                .stream()
                .sorted(Comparator.comparing(Account::name))
                .collect(toList());

        assertThat(accounts).hasSize(2);
        AccountAssertion.assertThat(accounts.get(0))
                .hasId()
                .hasBankId("iron")
                .hasName("Checking");
        AccountAssertion.assertThat(accounts.get(1))
                .hasId()
                .hasBankId("iron")
                .hasName("Saving");
    }

    @Test
    public void getAccount() {
        linkAccounts();

        List<Account> accounts = member.getAccounts()
                .stream()
                .sorted(Comparator.comparing(Account::name))
                .collect(toList());
        assertThat(accounts).hasSize(2);

        Account account = member.getAccount(accounts.get(0).id());
        AccountAssertion.assertThat(account)
                .hasId()
                .hasName("Checking");
    }

    private List<Account> link(String bankId, List<SealedMessage> payloads) {
        List<Account> accounts = member.linkAccounts(bankId, payloads);
        accounts.sort(Comparator.comparing(Account::name));
        return accounts;
    }
}
