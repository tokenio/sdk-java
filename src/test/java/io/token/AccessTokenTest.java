package io.token;

import com.google.common.collect.ImmutableSet;
import io.grpc.StatusRuntimeException;
import io.token.proto.PagedList;
import io.token.proto.common.member.MemberProtos.AddressRecord;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.proto.common.token.TokenProtos.TokenOperationResult.Status;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenSignature.Action;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import org.junit.Rule;
import org.junit.Test;

import static io.token.testing.sample.Sample.address;
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
    public void getAccessToken() {
        AddressRecord address = member1.addAddress(string(), address());
        Token accessToken = member1.createAccessToken(AccessTokenBuilder
                .create(member2.firstUsername())
                .forAddress(address.getId()));
        Token result = member1.getToken(accessToken.getId());
        assertThat(result).isEqualTo(accessToken);
    }

    @Test
    public void getAccessTokens() {
        AddressRecord address = member1.addAddress(string(), address());

        Token accessToken = member1.createAccessToken(AccessTokenBuilder
                .create(member1.firstUsername())
                .forAddress(address.getId()));

        PagedList<Token, String> result = member1.getAccessTokens(null, 2);
        assertThat(result.getList()).contains(accessToken);
    }

    @Test(expected = StatusRuntimeException.class)
    public void onlyOneAccessTokenAllowed() {
        AddressRecord address = member1.addAddress(string(), address());

        member1.createAccessToken(AccessTokenBuilder
                .create(member1.firstUsername())
                .forAddress(address.getId()));
        member1.createAccessToken(AccessTokenBuilder
                .create(member1.firstUsername())
                .forAddress(address.getId()));
    }

    @Test
    public void addressAccessToken_failNonEndorsed() {
        AddressRecord address = member1.addAddress(string(), address());
        Token accessToken = member1.createAccessToken(AccessTokenBuilder
                .create(member2.firstUsername())
                .forAddress(address.getId()));
        member2.useAccessToken(accessToken.getId());
        assertThatExceptionThrownBy(() ->
                member2.getAddress(address.getId())
        );
    }

    @Test
    public void addressAccessToken() {
        AddressRecord address1 = member1.addAddress(string(), address());
        Token accessToken = member1.createAccessToken(AccessTokenBuilder
                .create(member2.firstUsername())
                .forAddress(address1.getId()));
        TokenOperationResult res = member1.endorseToken(accessToken);
        assertThat(res.getStatus()).isEqualTo(Status.SUCCESS);

        assertThatExceptionThrownBy(() ->
                member2.getAddress(address1.getId())
        );

        member2.useAccessToken(accessToken.getId());
        AddressRecord result = member2.getAddress(address1.getId());
        assertThat(result).isEqualTo(address1);

        member2.clearAccessTokenOf();
        assertThatExceptionThrownBy(() ->
                member2.getAddress(address1.getId())
        );
    }

    @Test
    public void addressAccessToken_canceled() {
        AddressRecord address1 = member1.addAddress(string(), address());
        Token accessToken = member1.createAccessToken(AccessTokenBuilder
                .create(member2.firstUsername())
                .forAddress(address1.getId()));
        member1.endorseToken(accessToken);

        member2.useAccessToken(accessToken.getId());
        AddressRecord result = member2.getAddress(address1.getId());
        assertThat(result).isEqualTo(address1);

        TokenOperationResult res = member1.cancelToken(accessToken);
        assertThat(res.getStatus()).isEqualTo(Status.SUCCESS);

        assertThatExceptionThrownBy(() ->
                member2.getAddress(address1.getId())
        );
    }

    @Test
    public void createAddressAccessToken() {
        AddressRecord address1 = member1.addAddress(string(), address());
        AddressRecord address2 = member1.addAddress(string(), address());
        Token accessToken = member1.createAccessToken(AccessTokenBuilder
                .create(member2.firstUsername())
                .forAddress(address1.getId()));
        member1.endorseToken(accessToken);
        member2.useAccessToken(accessToken.getId());
        assertThatExceptionThrownBy(() ->
                member2.getAddress(address2.getId())
        );
    }

    @Test
    public void createAddressesAccessToken() {
        AddressRecord address1 = member1.addAddress(string(), address());
        AddressRecord address2 = member1.addAddress(string(), address());
        Token accessToken = member1.createAccessToken(AccessTokenBuilder
                .create(member2.firstUsername())
                .forAllAddresses());
        member1.endorseToken(accessToken);
        member2.useAccessToken(accessToken.getId());
        AddressRecord result = member2.getAddress(address2.getId());
        assertThat(result).isEqualTo(address2);
        assertThat(result).isNotEqualTo(address1);
    }

    @Test
    public void createBalancesAccessToken() {
        Account account = rule.account();
        Member accountMember = account.member();
        Token accessToken = accountMember.createAccessToken(AccessTokenBuilder
                .create(member1.firstUsername())
                .forAllBalances());
        accountMember.endorseToken(accessToken);

        assertThatExceptionThrownBy(() ->
                member1.getAccount(account.id())
        );

        member1.useAccessToken(accessToken.getId());
        Money balance = member1.getBalance(account.id());

        assertThat(balance).isEqualTo(account.getBalance());
    }

    @Test
    public void createAccountAccessToken() {
        Token accessToken = payerAccount.member().createAccessToken(AccessTokenBuilder
                .create(member2.firstUsername())
                .forAccount(payerAccount.id()));
        payerAccount.member().endorseToken(accessToken);
        member2.useAccessToken(accessToken.getId());
        Account result = member2.getAccount(payerAccount.id());

        assertThat(result.name()).isEqualTo(payerAccount.name());
    }

    @Test
    public void createAccountsAccessToken() {
        Token accessToken = payerAccount.member().createAccessToken(AccessTokenBuilder
                .create(member2.firstUsername())
                .forAllAccounts());
        payerAccount.member().endorseToken(accessToken);
        member2.useAccessToken(accessToken.getId());
        Account result = member2.getAccount(payerAccount.id());

        assertThat(result.name()).isEqualTo(payerAccount.name());
    }

    @Test
    public void createBalanceAccessToken() {
        Account account = rule.account();
        Member accountMember = account.member();
        Token accessToken = accountMember.createAccessToken(AccessTokenBuilder
                .create(member1.firstUsername())
                .forAccountBalances(account.id()));
        accountMember.endorseToken(accessToken);

        assertThatExceptionThrownBy(() ->
                member1.getAccount(account.id())
        );

        member1.useAccessToken(accessToken.getId());
        Money balance = member1.getBalance(account.id());

        assertThat(balance).isEqualTo(account.getBalance());
    }

    @Test
    public void createAccountTransactionsAccessToken() {
        Transaction transaction = getTransaction(payerAccount, payeeAccount);

        assertThatExceptionThrownBy(() ->
                member1.getTransaction(payerAccount.id(), transaction.getId())
        );

        Token accessToken = payerAccount.member().createAccessToken(AccessTokenBuilder
                .create(member1.firstUsername())
                .forAccountTransactions(payerAccount.id()));
        payerAccount.member().endorseToken(accessToken);
        member1.useAccessToken(accessToken.getId());
        Transaction result = member1.getTransaction(payerAccount.id(), transaction.getId());

        assertThat(result).isEqualTo(transaction);
    }

    @Test
    public void createAccountsTransactionsAccessToken() {
        Transaction transaction = getTransaction(payerAccount, payeeAccount);

        Token accessToken = payerAccount.member().createAccessToken(AccessTokenBuilder
                .create(member1.firstUsername())
                .forAllTransactions());
        payerAccount.member().endorseToken(accessToken);
        member1.useAccessToken(accessToken.getId());
        Transaction result = member1.getTransaction(payerAccount.id(), transaction.getId());

        assertThat(result).isEqualTo(transaction);
    }

    @Test
    public void accountAccess_getTransactions() {
        Transaction transaction = getTransaction(payerAccount, payeeAccount);

        Token accessToken = payerAccount.member().createAccessToken(AccessTokenBuilder
                .create(member1.firstUsername())
                .forAllTransactions());
        payerAccount.member().endorseToken(accessToken);
        member1.useAccessToken(accessToken.getId());
        PagedList<Transaction, String> result = member1.getTransactions(payerAccount.id(), null, 1);

        assertThat(result.getList()).contains(transaction);
        assertThat(result.getOffset()).isNotEmpty();
    }

    @Test
    public void accountAccess_getTransactionsPaged() {
        Token accessToken = payerAccount.member().createAccessToken(AccessTokenBuilder
                .create(member1.firstUsername())
                .forAllTransactions());
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

    @Test
    public void replaceAddressTokenAndEndorse() {
        Account account = rule.account();
        Member accountMember = account.member();
        Token accessToken = accountMember.createAccessToken(AccessTokenBuilder
                .create(member1.firstUsername())
                .forAllBalances());
        accountMember.endorseToken(accessToken);

        member1.useAccessToken(accessToken.getId());
        Money balance = member1.getBalance(account.id());
        assertThat(balance).isEqualTo(account.getBalance());

        TokenOperationResult result = accountMember.replaceAndEndorseAccessToken(
                accessToken,
                AccessTokenBuilder.from(accessToken.getPayload()).forAll());
        assertThat(result.getStatus()).isEqualTo(Status.SUCCESS);
        assertThat(result.getToken().getPayloadSignaturesList()
                .stream()
                .anyMatch(s -> s.getAction() == Action.ENDORSED)).isTrue();
    }

    @Test
    public void replaceAddressTokenNoEndorse() {
        Account account = rule.account();
        Member accountMember = account.member();
        Token accessToken = accountMember.createAccessToken(AccessTokenBuilder
                .create(member1.firstUsername())
                .forAllBalances());
        accountMember.endorseToken(accessToken);

        member1.useAccessToken(accessToken.getId());
        Money balance = member1.getBalance(account.id());
        assertThat(balance).isEqualTo(account.getBalance());

        TokenOperationResult result = accountMember.replaceAccessToken(
                accessToken,
                AccessTokenBuilder.from(accessToken.getPayload()).forAll());
        assertThat(result.getStatus()).isEqualTo(Status.MORE_SIGNATURES_NEEDED);
        assertThat(result.getToken().getPayloadSignaturesList().isEmpty()).isTrue();
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
        token = payer.endorseToken(token).getToken();
        Transfer transfer = payee.redeemToken(token, 1.0, "USD", "one");
        return payerAccount.getTransaction(transfer.getReferenceId());
    }

}
