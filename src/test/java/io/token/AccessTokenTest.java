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
                address1.getId());

        assertThatExceptionThrownBy( () ->
                member2.getAddress(address1.getId())
        );

        member2.useAccessToken(accessToken.getId());
        Address result = member2.getAddress(address1.getId());
        assertThat(result).isEqualTo(address1);

        member2.clearOnBehalfOf();
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
                address1.getId());
        member2.useAccessToken(accessToken.getId());
        assertThatExceptionThrownBy( () ->
                member2.getAddress(address2.getId())
        );
    }

    @Test
    public void addressAccessToken_withoutId() {
        Address address1 = member1.addAddress(string(), string());
        Address address2 = member1.addAddress(string(), string());
        AccessToken accessToken = member1.createAddressAccessToken(
                member2.firstAlias(),
                null);
        member2.useAccessToken(accessToken.getId());
        Address result = member2.getAddress(address2.getId());
        assertThat(result).isEqualTo(address2);
        assertThat(result).isNotEqualTo(address1);
    }

    @Test
    public void accountAccess_getBalance() {
        Account account = rule.account();
        Member accountMember = account.member();
        AccessToken accessToken = accountMember.createAccountAccessToken(
                member1.firstAlias(),
                account.id());

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

        AccessToken accessToken = payerAccount.member().createTransactionAccessToken(
                member1.firstAlias(),
                payerAccount.id());

        member1.useAccessToken(accessToken.getId());
        Transaction result = member1.getTransaction(payerAccount.id(), transaction.getId());

        assertThat(result).isEqualTo(transaction);
    }

    @Test
    public void accountAccess_getTransaction_wildcard() {
        Transaction transaction = getTransaction(payerAccount, payeeAccount);

        AccessToken accessToken = payerAccount.member().createTransactionAccessToken(
                member1.firstAlias(),
                null);

        member1.useAccessToken(accessToken.getId());
        Transaction result = member1.getTransaction(payerAccount.id(), transaction.getId());

        assertThat(result).isEqualTo(transaction);
    }

    @Test
    public void accountAccess_getTransactions() {
        Transaction transaction = getTransaction(payerAccount, payeeAccount);

        AccessToken accessToken = payerAccount.member().createTransactionAccessToken(
                member1.firstAlias(),
                null);

        member1.useAccessToken(accessToken.getId());
        List<Transaction> result = member1.getTransactions(payerAccount.id(), 0, 1);

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
