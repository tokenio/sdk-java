package io.token;

import static io.grpc.Status.Code.FAILED_PRECONDITION;
import static io.token.asserts.TransferAssertion.assertThat;
import static io.token.testing.sample.Sample.string;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.grpc.StatusRuntimeException;
import io.token.common.LinkedAccount;
import io.token.common.TokenRule;
import io.token.proto.PagedList;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transaction.TransactionProtos.TransactionStatus;
import io.token.proto.common.transfer.TransferProtos.Transfer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TransferRedemptionTest {
    @Rule public TokenRule rule = new TokenRule();

    private LinkedAccount payerAccount;
    private Member payer;
    private LinkedAccount payeeAccount;
    private Member payee;

    @Before
    public void before() {
        this.payerAccount = rule.linkedAccount();
        this.payer = payerAccount.getMember();
        this.payeeAccount = rule.linkedAccount();
        this.payee = payeeAccount.getMember();
    }

    @Test
    public void redeemToken() {
        Token token = token(100.0);
        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();

        Transfer transfer = payee.redeemToken(token);
        assertThat(transfer)
                .isProcessing()
                .hasNoAmount()
                .hasNSignatures(2)
                .isSignedBy(payee, Key.Level.LOW);
    }

    @Test
    public void redeemTokenWithUnlinkedAccount() {
        Token token = token(100.0);
        final Token endorsedToken = payer.endorseToken(token, Key.Level.STANDARD).getToken();
        payer.unlinkAccounts(singletonList(payerAccount.getId()));
        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(() -> payee.redeemToken(endorsedToken))
                .matches(e -> e.getStatus().getCode() == FAILED_PRECONDITION);
    }

    @Test
    public void redeemToken_withParams() {
        Token token = token(100.0);
        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();

        Transfer transfer = payee.redeemToken(
                token,
                99.0,
                payerAccount.getCurrency(),
                "transfer description");
        assertThat(transfer)
                .isProcessing()
                .hasAmount(99.0)
                .hasCurrency(payerAccount.getCurrency())
                .hasNSignatures(2)
                .isSignedBy(payee, Key.Level.LOW);
    }

    @Test
    public void redeemToken_failure() {
        double amount = payerAccount.getBalance() + 1; // 1 over the limit.

        Token token = token(amount);
        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();

        Transfer transfer = payee.redeemToken(
                token,
                amount,
                payerAccount.getCurrency(),
                "transfer description");
        assertThat(transfer)
                .hasStatus(TransactionStatus.FAILURE_INSUFFICIENT_FUNDS)
                .hasAmount(amount)
                .hasCurrency(payerAccount.getCurrency())
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
                .isProcessing()
                .isEqualTo(transfer);
    }

    @Test
    public void getTransfers() {
        Token token = token(100.0);
        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();

        Transfer transfer1 = payee.redeemToken(
                token,
                10.0,
                payerAccount.getCurrency(),
                "first");
        Transfer transfer2 = payee.redeemToken(
                token,
                20.0,
                payerAccount.getCurrency(),
                "second");
        Transfer transfer3 = payee.redeemToken(
                token,
                70.0,
                payerAccount.getCurrency(),
                "third");

        assertThat(transfer1)
                .isProcessing()
                .hasAmount(10.0)
                .hasCurrency(payerAccount.getCurrency())
                .hasDescription("first");
        assertThat(transfer2)
                .isProcessing()
                .hasAmount(20.0)
                .hasCurrency(payerAccount.getCurrency())
                .hasDescription("second");
        assertThat(transfer3)
                .isProcessing()
                .hasAmount(70.0)
                .hasCurrency(payerAccount.getCurrency())
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
        return payerAccount.createTransferToken(amount, payeeAccount)
                .setRedeemerUsername(payee.firstUsername())
                .addDestination(Destinations.sepa(string()))
                .execute();
    }
}
