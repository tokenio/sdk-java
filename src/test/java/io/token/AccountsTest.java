package io.token;

import io.token.asserts.AccountAssertion;
import io.token.proto.ProtoJson;
import io.token.proto.common.account.AccountProtos.AccountLinkPayload;
import io.token.util.codec.ByteEncoding;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class AccountsTest {
    @Rule
    public TokenRule rule = new TokenRule();
    private final Member member = rule.member();

    @Test
    public void linkAccounts() {
        String bankId = "bank-id";

        byte[] checking = ProtoJson.toJson(AccountLinkPayload.newBuilder()
                .setUsername(member.firstUsername())
                .setExpirationMs(System.currentTimeMillis() + 10000)
                .setAccountName("Checking")
                .setAccountNumber("iban:checking")
                .build()).getBytes();

        byte[] saving = ProtoJson.toJson(AccountLinkPayload.newBuilder()
                .setUsername(member.firstUsername())
                .setExpirationMs(System.currentTimeMillis() + 10000)
                .setAccountName("Saving")
                .setAccountNumber("iban:saving")
                .build()).getBytes();

        List<String> payloads = Stream.of(checking, saving)
                .map(ByteEncoding::serialize)
                .collect(toList());

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
    public void getAccounts() {
        linkAccounts();

        List<Account> accounts = member.getAccounts()
                .stream()
                .sorted((a1, a2) -> a1.name().compareTo(a2.name()))
                .collect(toList());

        assertThat(accounts).hasSize(2);
        AccountAssertion.assertThat(accounts.get(0))
                .hasId()
                .hasName("Checking");
        AccountAssertion.assertThat(accounts.get(1))
                .hasId()
                .hasName("Saving");
    }

    @Test
    public void getAccount() {
        linkAccounts();

        List<Account> accounts = member.getAccounts()
                .stream()
                .sorted((a1, a2) -> a1.name().compareTo(a2.name()))
                .collect(toList());
        assertThat(accounts).hasSize(2);

        Account account = member.getAccount(accounts.get(0).id());
        AccountAssertion.assertThat(account)
                .hasId()
                .hasName("Checking");
    }

    private List<Account> link(String bankId, List<String> payloads) {
        List<Account> accounts = member.linkAccounts(bankId, payloads);
        accounts.sort((a1, a2) -> a1.name().compareTo(a2.name()));
        return accounts;
    }
}
