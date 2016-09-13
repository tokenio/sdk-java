package io.token;

import io.token.asserts.AccountAssertion;
import io.token.proto.ProtoJson;
import io.token.proto.common.account.AccountProtos.AccountLinkPayload;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static com.sun.tools.attach.VirtualMachine.list;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class AccountTest {
    @Rule public TokenRule rule = new TokenRule();
    private final Member member = rule.member();

    @Test
    public void linkAccounts() {
        String alias = member.getAliases().get(0);
        String bankId = "bank-id";

        List<Account> accounts = member.linkAccount(
                bankId,
                ProtoJson.toJson(AccountLinkPayload.newBuilder()
                        .setAlias(alias)
                        .addAccounts(AccountLinkPayload.NamedAccount.newBuilder()
                                .setName("Checking")
                                .setAccountNumber("iban:checking"))
                        .addAccounts(AccountLinkPayload.NamedAccount.newBuilder()
                                .setName("Savings")
                                .setAccountNumber("iban:savings"))
                        .build()).getBytes());

        assertThat(accounts).hasSize(2);
        AccountAssertion.assertThat(accounts.get(0).getAccount())
                .hasId()
                .hasName("Checking");
        AccountAssertion.assertThat(accounts.get(1).getAccount())
                .hasId()
                .hasName("Savings");
    }

    @Test
    public void lookupAccounts() {
        linkAccounts();

        List<Account> accounts = member.lookupAccounts()
                .stream()
                .sorted((a1, a2) -> a1.getAccount().getName().compareTo(a2.getAccount().getName()))
                .collect(toList());

        assertThat(accounts).hasSize(2);
        AccountAssertion.assertThat(accounts.get(0).getAccount())
                .hasId()
                .hasName("Checking");
        AccountAssertion.assertThat(accounts.get(1).getAccount())
                .hasId()
                .hasName("Savings");

        for (Account account : accounts) {
            account.setAccountName("New " + account.getAccount().getName());
        }
        AccountAssertion.assertThat(accounts.get(0).getAccount())
                .hasId()
                .hasName("New Checking");
        AccountAssertion.assertThat(accounts.get(1).getAccount())
                .hasId()
                .hasName("New Savings");
    }
}
