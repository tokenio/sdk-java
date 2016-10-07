package io.token;

import io.token.proto.common.member.MemberProtos.Address;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.payment.PaymentProtos;
import io.token.proto.common.token.TokenProtos.AccessToken;
import io.token.proto.common.token.TokenProtos.PaymentToken;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

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
    public void addressAccessToken() {
        Address address1 = member1.addAddress(string(), string());
        AccessToken accessToken = member1.createAddressAccessToken(
                member2.firstAlias(),
                Optional.of(address1.getId()));

        assertThatExceptionThrownBy( () ->
                member2.getAddress(address1.getId())
        );

        Address result = member2.getAddress(address1.getId(), accessToken.getId());
        assertThat(result).isEqualTo(address1);

        assertThatExceptionThrownBy( () ->
                member2.getAddress(address1.getId())
        );
    }

    @Test
    public void addressAccessToken_withAddressId() {
        Address address1 = member1.addAddress(string(), string());
        Address address2 = member1.addAddress(string(), string());
        AccessToken accessToken = member1.createAddressAccessToken(
                member2.firstAlias(),
                Optional.of(address1.getId()));
        assertThatExceptionThrownBy( () ->
                member2.getAddress(address2.getId(), accessToken.getId())
        );
    }

    @Test
    public void addressAccessToken_withoutId() {
        Address address1 = member1.addAddress(string(), string());
        Address address2 = member1.addAddress(string(), string());
        AccessToken accessToken = member1.createAddressAccessToken(
                member2.firstAlias(),
                Optional.empty());
        Address result = member2.getAddress(address2.getId(), accessToken.getId());
        assertThat(result).isEqualTo(address2);
        assertThat(result).isNotEqualTo(address1);
    }

    @Test
    public void accountAccess_getBalance() {
        Account account = rule.account();
        Member accountMember = account.member();
        AccessToken accessToken = accountMember.createAccountAccessToken(
                member1.firstAlias(),
                Optional.of(account.id()));

        assertThatExceptionThrownBy( () ->
                member1.getAccount(account.id())
        );

        String onBehalfOf = accessToken.getId();
        Account resultAccount = member1.getAccount(account.id(), onBehalfOf);
        Money balance = resultAccount.getBalance();

        assertThat(balance).isEqualTo(account.getBalance());
    }

    @Test
    public void accountAccess_getTransaction() {
        Transaction transaction = getTransaction(payerAccount, payeeAccount);

        assertThatExceptionThrownBy( () ->
            member1.getTransaction(payerAccount.id(), transaction.getId())
        );

        AccessToken accessToken = payerAccount.member().createTransactionAccessToken(
                member1.firstAlias(),
                Optional.of(payerAccount.id()));

        String onBehalfOf = accessToken.getId();
        Transaction result = member1.getTransaction(payerAccount.id(), transaction.getId(), onBehalfOf);

        assertThat(result).isEqualTo(transaction);
    }

    @Test
    public void accountAccess_getTransaction_wildcard() {
        Transaction transaction = getTransaction(payerAccount, payeeAccount);

        AccessToken accessToken = payerAccount.member().createTransactionAccessToken(
                member1.firstAlias(),
                Optional.empty());

        String onBehalfOf = accessToken.getId();
        Transaction result = member1.getTransaction(payerAccount.id(), transaction.getId(), onBehalfOf);

        assertThat(result).isEqualTo(transaction);
    }

    @Test
    public void accountAccess_getTransactions() {
        Transaction transaction = getTransaction(payerAccount, payeeAccount);

        AccessToken accessToken = payerAccount.member().createTransactionAccessToken(
                member1.firstAlias(),
                Optional.empty());

        String onBehalfOf = accessToken.getId();
        List<Transaction> result = member1.getTransactions(payerAccount.id(), 0, 1, onBehalfOf);

        assertThat(result).contains(transaction);
    }

    private Transaction getTransaction(Account payerAccount, Account payeeAccount) {
        Member payer = payerAccount.member();
        Member payee = payeeAccount.member();
        PaymentToken token = payer.createPaymentToken(
                10.0,
                "USD",
                payerAccount.id(),
                payee.firstAlias(),
                string());
        token = payer.endorsePaymentToken(token);
        PaymentProtos.Payment payment = payee.redeemPaymentToken(token, 1.0, "USD");
        return payerAccount.getTransaction(payment.getReferenceId());
    }
}
