package io.token;

import static io.grpc.Status.Code.FAILED_PRECONDITION;
import static io.token.asserts.TransferAssertion.assertThat;
import static io.token.testing.sample.Sample.string;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.grpc.StatusRuntimeException;
import io.token.common.TokenRule;
import io.token.proto.PagedList;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transaction.TransactionProtos.TransactionStatus;
import io.token.proto.common.transfer.TransferProtos.Transfer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TransferRedemptionTest {
    @Rule public TokenRule rule = new TokenRule();

    private Account payerAccount;
    private Member payer;
    private Member payee;

    @Before
    public void before() {
        this.payerAccount = rule.account();
        this.payer = payerAccount.member();

        Account payeeAccount = rule.account();
        this.payee = payeeAccount.member();
    }

    @Test
    public void redeemToken() {
        Token token = token(100.0);
        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();

        Transfer transfer = payee.redeemToken(token);
        assertThat(transfer)
                .isSuccessful()
                .hasNoAmount()
                .hasNSignatures(2)
                .isSignedBy(payee, Key.Level.LOW);
    }

    @Test
    public void redeemTokenWithUnlinkedAccount() {
        Token token = token(100.0);
        final Token endorsedToken = payer.endorseToken(token, Key.Level.STANDARD).getToken();
        payer.unlinkAccounts(singletonList(payerAccount.id()));
        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(() -> payee.redeemToken(endorsedToken))
                .matches(e -> e.getStatus().getCode() == FAILED_PRECONDITION);
    }

    @Test
    public void redeemToken_withParams() {
        Token token = token(100.0);
        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();

        Transfer transfer = payee.redeemToken(token, 99.0, "USD", "transfer description");
        assertThat(transfer)
                .isSuccessful()
                .hasAmount(99.0)
                .hasCurrency("USD")
                .hasNSignatures(2)
                .isSignedBy(payee, Key.Level.LOW);
    }

    @Test
    public void redeemToken_failure() {
        Money balance = payerAccount.getBalance();
        double amount = Double.parseDouble(balance.getValue()) + 1; // 1 over the limit.

        Token token = token(amount);
        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();

        Transfer transfer = payee.redeemToken(token, amount, "USD", "transfer description");
        assertThat(transfer)
                .hasStatus(TransactionStatus.FAILURE_INSUFFICIENT_FUNDS)
                .hasAmount(amount)
                .hasCurrency("USD")
                .hasNSignatures(2)
                .isSignedBy(payee, Key.Level.LOW);
    }

    @Test
    public void getTransfer() {
        Token token = token(100.0);
        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();

        Transfer transfer = payee.redeemToken(token);
        Transfer lookedUp = payer.getTransfer(transfer.getId());
        assertThat(lookedUp)
                .isSuccessful()
                .isEqualTo(transfer);
    }

    @Test
    public void getTransfers() {
        Token token = token(100.0);
        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();

        Transfer transfer1 = payee.redeemToken(token, 10.0, "USD", "first");
        Transfer transfer2 = payee.redeemToken(token, 20.0, "USD", "second");
        Transfer transfer3 = payee.redeemToken(token, 70.0, "USD", "third");

        assertThat(transfer1)
                .isSuccessful()
                .hasAmount(10.0)
                .hasCurrency("USD")
                .hasDescription("first");
        assertThat(transfer2)
                .isSuccessful()
                .hasAmount(20.0)
                .hasCurrency("USD")
                .hasDescription("second");
        assertThat(transfer3)
                .isSuccessful()
                .hasAmount(70.0)
                .hasCurrency("USD")
                .hasDescription("third");

        PagedList<Transfer, String> lookedUp = payer.getTransfers(null, 100, token.getId());
        assertThat(lookedUp.getList()).containsOnly(transfer1, transfer2, transfer3);
        assertThat(lookedUp.getOffset()).isNotEmpty();

        // Make sure the same results are returned if no token ID was provided
        lookedUp = payer.getTransfers(null, 100, null);
        assertThat(lookedUp.getList()).containsOnly(transfer1, transfer2, transfer3);
        assertThat(lookedUp.getOffset()).isNotEmpty();
    }

    private Token token(double amount) {
        return payer.createTransferToken(amount, "USD")
                .setAccountId(payerAccount.id())
                .setRedeemerUsername(payee.firstUsername())
                .addDestination(Destinations.sepa(string()))
                .execute();
    }
}
