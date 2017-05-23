package io.token;

import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.testing.sample.Sample.address;
import static io.token.testing.sample.Sample.string;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableSet;
import io.grpc.StatusRuntimeException;
import io.token.common.TokenRule;
import io.token.proto.PagedList;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.member.MemberProtos.AddressRecord;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.token.TokenProtos;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.proto.common.token.TokenProtos.TokenOperationResult.Status;
import io.token.proto.common.token.TokenProtos.TokenSignature.Action;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferEndpoint;
import io.token.testing.sample.Sample;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class AccessTokenTest {
    @Rule
    public TokenRule rule = new TokenRule();

    private Member member1;
    private Member member2;
    private Account payerAccount;
    private Account payeeAccount;

    @Before
    public void before() {
        this.member1 = rule.member();
        this.member2 = rule.member();
        this.payerAccount = rule.account();
        this.payeeAccount = rule.account();
    }

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
    public void createAccessTokenIdempotent() {
        AddressRecord address = member1.addAddress(string(), address());

        AccessTokenBuilder builder = AccessTokenBuilder
                .create(member1.firstUsername())
                .forAddress(address.getId());

        member1.createAccessToken(builder);
        member1.createAccessToken(builder);
        assertThat(member1.getAccessTokens(null, 2).getList().size()).isEqualTo(1);
    }

    @Test
    public void addressAccessToken_failNonEndorsed() {
        final AddressRecord address = member1.addAddress(Sample.string(), address());
        Token accessToken = member1.createAccessToken(AccessTokenBuilder
                .create(member2.firstUsername())
                .forAddress(address.getId()));
        member2.useAccessToken(accessToken.getId());

        assertThatThrownBy(() -> member2.getAddress(address.getId()));
    }

    @Test
    public void addressAccessToken() {
        final AddressRecord address = member1.addAddress(string(), address());
        Token accessToken = member1.createAccessToken(AccessTokenBuilder
                .create(member2.firstUsername())
                .forAddress(address.getId()));
        TokenOperationResult res = member1.endorseToken(accessToken, STANDARD);
        assertThat(res.getStatus()).isEqualTo(Status.SUCCESS);

        assertThatThrownBy(() -> member2.getAddress(address.getId()));

        member2.useAccessToken(accessToken.getId());
        AddressRecord result = member2.getAddress(address.getId());
        assertThat(result).isEqualTo(address);

        member2.clearAccessToken();
        assertThatThrownBy(() -> member2.getAddress(address.getId()));
    }

    @Test
    public void addressAccessToken_canceled() {
        final AddressRecord address = member1.addAddress(string(), address());
        Token accessToken = member1.createAccessToken(AccessTokenBuilder
                .create(member2.firstUsername())
                .forAddress(address.getId()));
        member1.endorseToken(accessToken, STANDARD);

        member2.useAccessToken(accessToken.getId());
        AddressRecord result = member2.getAddress(address.getId());
        assertThat(result).isEqualTo(address);

        TokenOperationResult res = member1.cancelToken(accessToken);
        assertThat(res.getStatus()).isEqualTo(Status.SUCCESS);

        assertThatThrownBy(() -> member2.getAddress(address.getId()));
    }

    @Test
    public void createAddressAccessToken() {
        final AddressRecord address1 = member1.addAddress(string(), address());
        final AddressRecord address2 = member1.addAddress(string(), address());
        Token accessToken = member1.createAccessToken(AccessTokenBuilder
                .create(member2.firstUsername())
                .forAddress(address1.getId()));
        member1.endorseToken(accessToken, STANDARD);
        member2.useAccessToken(accessToken.getId());

        assertThatThrownBy(() -> member2.getAddress(address2.getId()));
    }

    @Test
    public void createAddressesAccessToken() {
        Token accessToken = member1.createAccessToken(AccessTokenBuilder
                .create(member2.firstUsername())
                .forAllAddresses());
        member1.endorseToken(accessToken, STANDARD);
        member2.useAccessToken(accessToken.getId());
        AddressRecord address1 = member1.addAddress(string(), address());
        AddressRecord address2 = member1.addAddress(string(), address());
        AddressRecord result = member2.getAddress(address2.getId());
        assertThat(result).isEqualTo(address2);
        assertThat(result).isNotEqualTo(address1);
    }

    @Test
    public void createBalancesAccessToken() {
        final Account account = rule.account();
        Member accountMember = account.member();
        Token accessToken = accountMember.createAccessToken(AccessTokenBuilder
                .create(member1.firstUsername())
                .forAllBalances());
        accountMember.endorseToken(accessToken, STANDARD);

        assertThatThrownBy(() -> member1.getAccount(account.id()));

        member1.useAccessToken(accessToken.getId());
        Money balance = member1.getBalance(account.id());

        assertThat(balance).isEqualTo(account.getBalance());
    }

    @Test
    public void createAccountAccessToken() {
        Token accessToken = payerAccount.member().createAccessToken(AccessTokenBuilder
                .create(member2.firstUsername())
                .forAccount(payerAccount.id()));
        payerAccount.member().endorseToken(accessToken, STANDARD);
        member2.useAccessToken(accessToken.getId());
        Account result = member2.getAccount(payerAccount.id());

        assertThat(result.name()).isEqualTo(payerAccount.name());
    }

    @Test
    public void createAccountsAccessToken() {
        Token accessToken = payerAccount.member().createAccessToken(AccessTokenBuilder
                .create(member2.firstUsername())
                .forAllAccounts());
        payerAccount.member().endorseToken(accessToken, STANDARD);
        member2.useAccessToken(accessToken.getId());
        Account result = member2.getAccount(payerAccount.id());

        assertThat(result.name()).isEqualTo(payerAccount.name());
    }

    @Test
    public void createBalanceAccessToken() {
        final Account account = rule.account();
        Member accountMember = account.member();
        Token accessToken = accountMember.createAccessToken(AccessTokenBuilder
                .create(member1.firstUsername())
                .forAccountBalances(account.id()));
        accountMember.endorseToken(accessToken, STANDARD);

        assertThatThrownBy(() -> member1.getAccount(account.id()));

        member1.useAccessToken(accessToken.getId());
        Money balance = member1.getBalance(account.id());

        assertThat(balance).isEqualTo(account.getBalance());
    }

    @Test
    public void createAccountTransactionsAccessToken() {
        final Transaction transaction = getTransaction(payerAccount, payeeAccount);

        assertThatThrownBy(() -> member1.getTransaction(payerAccount.id(), transaction.getId()));

        Token accessToken = payerAccount.member().createAccessToken(AccessTokenBuilder
                .create(member1.firstUsername())
                .forAccountTransactions(payerAccount.id()));
        payerAccount.member().endorseToken(accessToken, STANDARD);
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

        payerAccount.member().endorseToken(accessToken, STANDARD);
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
        payerAccount.member().endorseToken(accessToken, STANDARD);
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
        payerAccount.member().endorseToken(accessToken, STANDARD);

        int num = 10;
        for (int i = 0; i < num; i++) {
            getTransaction(payerAccount, payeeAccount);
        }

        int limit = 2;
        ImmutableSet.Builder<Transaction> builder = ImmutableSet.builder();
        member1.useAccessToken(accessToken.getId());
        PagedList<Transaction, String> result = member1.getTransactions(
                payerAccount.id(),
                null,
                limit);
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
        accountMember.endorseToken(accessToken, STANDARD);

        member1.useAccessToken(accessToken.getId());
        Money balance = member1.getBalance(account.id());
        assertThat(balance).isEqualTo(account.getBalance());

        TokenOperationResult result = accountMember.replaceAndEndorseAccessToken(
                accessToken,
                AccessTokenBuilder.fromPayload(accessToken.getPayload()).forAll());
        assertThat(result.getStatus()).isEqualTo(Status.SUCCESS);

        boolean hasEndorsed = false;

        for (TokenProtos.TokenSignature signature : result.getToken().getPayloadSignaturesList()) {
            Action action = signature.getAction();
            if (action == Action.ENDORSED) {
                hasEndorsed = true;
                break;
            }
        }

        assertThat(hasEndorsed).isTrue();
        member1.clearAccessToken();
        assertThat(member1.getToken(accessToken.getId()).getReplacedByTokenId())
                .isEqualTo(result.getToken().getId());
    }

    @Test(expected = StatusRuntimeException.class)
    public void replaceAddressTokenRace() {
        Account account = rule.account();
        Member accountMember = account.member();
        Token accessToken = accountMember.createAccessToken(AccessTokenBuilder
                .create(member1.firstUsername())
                .forAllBalances());
        accountMember.endorseToken(accessToken, STANDARD);

        member1.useAccessToken(accessToken.getId());
        Money balance = member1.getBalance(account.id());
        assertThat(balance).isEqualTo(account.getBalance());

        accountMember.replaceAndEndorseAccessToken(
                accessToken,
                AccessTokenBuilder.fromPayload(accessToken.getPayload()).forAll());
        accountMember.replaceAndEndorseAccessToken(
                accessToken,
                AccessTokenBuilder.fromPayload(accessToken.getPayload()).forAll());
        assertThat(member1.getAccessTokens(null, 2).getList().size()).isEqualTo(1);
    }

    @Test
    public void replaceAddressTokenIdempotent() {
        Account account = rule.account();
        Member accountMember = account.member();
        Token accessToken = accountMember.createAccessToken(AccessTokenBuilder
                .create(member1.firstUsername())
                .forAllBalances());
        accountMember.endorseToken(accessToken, STANDARD);

        member1.useAccessToken(accessToken.getId());
        Money balance = member1.getBalance(account.id());
        assertThat(balance).isEqualTo(account.getBalance());

        AccessTokenBuilder builder = AccessTokenBuilder
                .fromPayload(accessToken.getPayload())
                .forAll();
        accountMember.replaceAndEndorseAccessToken(accessToken, builder);
        accountMember.replaceAndEndorseAccessToken(accessToken, builder);
        assertThat(accountMember.getAccessTokens(null, 2).getList().size())
                .isEqualTo(1);
    }

    @Test
    public void replaceTokenLoop() {
        // Replace token with a new one then replace once more to get back to the old one.
        Account account = rule.account();
        Member accountMember = account.member();
        Token originalToken = accountMember.createAccessToken(AccessTokenBuilder
                .create(member1.firstUsername())
                .forAllBalances());
        accountMember.endorseToken(originalToken, STANDARD);

        member1.useAccessToken(originalToken.getId());
        Money balance = member1.getBalance(account.id());
        assertThat(balance).isEqualTo(account.getBalance());

        Token replacedToken = accountMember.replaceAndEndorseAccessToken(
                originalToken,
                AccessTokenBuilder.fromPayload(originalToken.getPayload()).forAll()).getToken();

        member1.useAccessToken(replacedToken.getId());
        balance = member1.getBalance(account.id());
        assertThat(balance).isEqualTo(account.getBalance());

        Token likeOriginal = accountMember.replaceAndEndorseAccessToken(
                replacedToken,
                AccessTokenBuilder.fromPayload(replacedToken.getPayload()).forAllBalances())
                .getToken();

        member1.useAccessToken(likeOriginal.getId());
        balance = member1.getBalance(account.id());
        assertThat(balance).isEqualTo(account.getBalance());
    }

    @Test
    public void replaceAddressTokenNoEndorse() {
        Account account = rule.account();
        Member accountMember = account.member();
        Token accessToken = accountMember.createAccessToken(AccessTokenBuilder
                .create(member1.firstUsername())
                .forAllBalances());
        accountMember.endorseToken(accessToken, STANDARD);

        member1.useAccessToken(accessToken.getId());
        Money balance = member1.getBalance(account.id());
        assertThat(balance).isEqualTo(account.getBalance());

        AccessTokenBuilder builder = AccessTokenBuilder.fromPayload(accessToken.getPayload())
                .forAll();
        assertThat(accessToken.getPayload().getNonce()).isNotEqualTo(builder.build().getNonce());

        TokenOperationResult result = accountMember.replaceAccessToken(accessToken, builder);
        assertThat(result.getStatus()).isEqualTo(Status.MORE_SIGNATURES_NEEDED);
        assertThat(result.getToken().getPayloadSignaturesList().isEmpty()).isTrue();
    }

    private Transaction getTransaction(Account payerAccount, Account payeeAccount) {
        Member payer = payerAccount.member();
        Member payee = payeeAccount.member();
        Token token = payer.createTransferToken(10, "USD")
                .setAccountId(payerAccount.id())
                .setRedeemerUsername(payee.firstUsername())
                .execute();
        token = payer.endorseToken(token, STANDARD).getToken();
        Transfer transfer = payee.redeemToken(token, 1.0, "USD", "one",
                TransferEndpoint.newBuilder()
                        .setAccount(BankAccount.newBuilder()
                                .setToken(BankAccount.Token.newBuilder()
                                        .setMemberId(payee.memberId())
                                        .setAccountId(payeeAccount.id())))
                        .build());
        return payerAccount.getTransaction(transfer.getReferenceId());
    }
}
