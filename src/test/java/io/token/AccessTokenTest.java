package io.token;

import com.google.common.collect.ImmutableSet;
import io.token.proto.PagedList;
import io.token.proto.common.member.MemberProtos.Address;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import org.junit.Rule;
import org.junit.Test;

import static io.token.testing.sample.Sample.string;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionThrownBy;

public class AccessTokenTest {
    @Rule
    public TokenRule rule = new TokenRule();
    private Member member1 = rule.member();
    private Member member2 = rule.member();
    private Account payerAccount = rule.account();
    private Account payeeAccount = rule.account();

    @Test
    public void addressAccessToken_failNonEndorsed() {
        Address address = member1.addAddress(string(), string());
        Token accessToken = member1.createAddressAccessToken(
                member2.firstUsername(),
                address.getId());
        member2.useAccessToken(accessToken.getId());
        assertThatExceptionThrownBy( () ->
                member2.getAddress(address.getId())
        );
    }

    @Test
    public void addressAccessToken() {
        Address address1 = member1.addAddress(string(), string());
        Token accessToken = member1.createAddressAccessToken(
                member2.firstUsername(),
                address1.getId());
        member1.endorseToken(accessToken);

        assertThatExceptionThrownBy( () ->
                member2.getAddress(address1.getId())
        );

        member2.useAccessToken(accessToken.getId());
        Address result = member2.getAddress(address1.getId());
        assertThat(result).isEqualTo(address1);

        member2.clearAccessTokenOf();
        assertThatExceptionThrownBy( () ->
                member2.getAddress(address1.getId())
        );
    }

    @Test
    public void addressAccessToken_withAddressId() {
        Address address1 = member1.addAddress(string(), string());
        Address address2 = member1.addAddress(string(), string());
        Token accessToken = member1.createAddressAccessToken(
                member2.firstUsername(),
                address1.getId());
        member1.endorseToken(accessToken);
        member2.useAccessToken(accessToken.getId());
        assertThatExceptionThrownBy( () ->
                member2.getAddress(address2.getId())
        );
    }

    @Test
    public void addressAccessToken_withoutId() {
        Address address1 = member1.addAddress(string(), string());
        Address address2 = member1.addAddress(string(), string());
        Token accessToken = member1.createAddressAccessToken(
                member2.firstUsername(),
                null);
        member1.endorseToken(accessToken);
        member2.useAccessToken(accessToken.getId());
        Address result = member2.getAddress(address2.getId());
        assertThat(result).isEqualTo(address2);
        assertThat(result).isNotEqualTo(address1);
    }

    @Test
    public void accountAccess_getBalance() {
        Account account = rule.account();
        Member accountMember = account.member();
        Token accessToken = accountMember.createAccountAccessToken(
                member1.firstUsername(),
                account.id());
        accountMember.endorseToken(accessToken);

        assertThatExceptionThrownBy( () ->
                member1.getAccount(account.id())
        );

        member1.useAccessToken(accessToken.getId());
        Account resultAccount = member1.getAccount(account.id());
        Money balance = resultAccount.getBalance();

        assertThat(balance).isEqualTo(account.getBalance());
    }

    @Test
    public void accountAccess_getTransaction() {
        Transaction transaction = getTransaction(payerAccount, payeeAccount);

        assertThatExceptionThrownBy( () ->
            member1.getTransaction(payerAccount.id(), transaction.getId())
        );

        Token accessToken = payerAccount.member().createTransactionAccessToken(
                member1.firstUsername(),
                payerAccount.id());
        payerAccount.member().endorseToken(accessToken);
        member1.useAccessToken(accessToken.getId());
        Transaction result = member1.getTransaction(payerAccount.id(), transaction.getId());

        assertThat(result).isEqualTo(transaction);
    }

    @Test
    public void accountAccess_getTransaction_wildcard() {
        Transaction transaction = getTransaction(payerAccount, payeeAccount);

        Token accessToken = payerAccount.member().createTransactionAccessToken(
                member1.firstUsername(),
                null);
        payerAccount.member().endorseToken(accessToken);
        member1.useAccessToken(accessToken.getId());
        Transaction result = member1.getTransaction(payerAccount.id(), transaction.getId());

        assertThat(result).isEqualTo(transaction);
    }

    @Test
    public void accountAccess_getTransactions() {
        Transaction transaction = getTransaction(payerAccount, payeeAccount);

        Token accessToken = payerAccount.member().createTransactionAccessToken(
                member1.firstUsername(),
                null);
        payerAccount.member().endorseToken(accessToken);
        member1.useAccessToken(accessToken.getId());
        PagedList<Transaction, String> result = member1.getTransactions(payerAccount.id(), null, 1);

        assertThat(result.getList()).contains(transaction);
        assertThat(result.getOffset()).isNotEmpty();
    }

    @Test
    public void accountAccess_getTransactionsPaged() {
        Token accessToken = payerAccount.member().createTransactionAccessToken(
                member1.firstUsername(),
                null);
        payerAccount.member().endorseToken(accessToken);

        int num = 10;
        for (int i = 0; i < num; i++) {
            getTransaction(payerAccount, payeeAccount);
        }

        int limit = 2;
        ImmutableSet.Builder<Transaction> builder = ImmutableSet.builder();
        member1.useAccessToken(accessToken.getId());
        PagedList<Transaction, String> result = member1.getTransactions(payerAccount.id(), null, limit);
        for (int i = 0; i < num / limit; i++) {
            builder.addAll(result.getList());
            result = member1.getTransactions(payerAccount.id(), result.getOffset(), limit);
        }

        assertThat(builder.build().size()).isEqualTo(num);
    }

    private Transaction getTransaction(Account payerAccount, Account payeeAccount) {
        Member payer = payerAccount.member();
        Member payee = payeeAccount.member();
        Token token = payer.createToken(
                10.0,
                "USD",
                payerAccount.id(),
                payee.firstUsername(),
                string());
        token = payer.endorseToken(token);
        Transfer transfer = payee.redeemToken(token, 1.0, "USD");
        return payerAccount.getTransaction(transfer.getReferenceId());
    }
}
