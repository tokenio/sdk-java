package io.token;

import io.token.asserts.AccountAssertion;
import io.token.proto.ProtoJson;
import io.token.proto.common.account.AccountProtos.AccountLinkPayload;
import io.token.util.codec.ByteEncoding;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class AccountsTest {
    @Rule public TokenRule rule = new TokenRule();
    private final Member member = rule.member();

    @Test
    public void linkAccounts() {
        String alias = member.getAliases().get(0);
        String bankId = "bank-id";

        byte[] data = ProtoJson.toJson(AccountLinkPayload.newBuilder()
                .setAlias(alias)
                .addAccounts(AccountLinkPayload.NamedAccount.newBuilder()
                        .setName("Checking")
                        .setAccountNumber("iban:checking"))
                .addAccounts(AccountLinkPayload.NamedAccount.newBuilder()
                        .setName("Savings")
                        .setAccountNumber("iban:savings"))
                .build()).getBytes();
        String accountLinkingPayload = ByteEncoding.serialize(data);

        List<Account> accounts = member.linkAccounts(bankId, accountLinkingPayload);

        assertThat(accounts).hasSize(2);
        AccountAssertion.assertThat(accounts.get(0))
                .hasId()
                .hasName("Checking");
        AccountAssertion.assertThat(accounts.get(1))
                .hasId()
                .hasName("Savings");
    }

    @Test
    public void lookupAccounts() {
        linkAccounts();

        List<Account> accounts = member.lookupAccounts()
                .stream()
                .sorted((a1, a2) -> a1.getName().compareTo(a2.getName()))
                .collect(toList());

        assertThat(accounts).hasSize(2);
        AccountAssertion.assertThat(accounts.get(0))
                .hasId()
                .hasName("Checking");
        AccountAssertion.assertThat(accounts.get(1))
                .hasId()
                .hasName("Savings");

        for (Account account : accounts) {
            account.setAccountName("New " + account.getName());
        }
        AccountAssertion.assertThat(accounts.get(0))
                .hasId()
                .hasName("New Checking");
        AccountAssertion.assertThat(accounts.get(1))
                .hasId()
                .hasName("New Savings");
    }
}
