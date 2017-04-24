package io.token;

import static io.token.asserts.TransferAssertion.assertThat;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.token.proto.PagedList;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transaction.TransactionProtos.TransactionStatus;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.Destination;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.Destination.TokenDestination;

import java.util.Collections;
import org.assertj.core.api.ThrowableAssert;
import org.junit.Rule;
import org.junit.Test;

public class TransferRedemptionTest {
    @Rule public TokenRule rule = new TokenRule();
    private final Account payerAccount = rule.account();
    private final Account payeeAccount = rule.account();
    private final Member payer = payerAccount.member();
    private final Member payee = payeeAccount.member();

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
        assertThatThrownBy(
                new ThrowableAssert.ThrowingCallable() {
                    public void call() throws Throwable {
                        payee.redeemToken(endorsedToken);
                    }
                })
                .isInstanceOf(StatusRuntimeException.class)
                .extracting("status")
                .extracting("code", Status.Code.class)
                .extractingResultOf("value", int.class)
                .contains(Status.Code.FAILED_PRECONDITION.value());
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
        return payer.createToken(
                amount,
                "USD",
                payerAccount.id(),
                payee.firstUsername(),
                "book purchase",
                Collections.singletonList(Destination.newBuilder()
                        .setTokenDestination(TokenDestination.newBuilder()
                                .setAccountId(payeeAccount.id())
                                .setMemberId(payee.memberId())
                                .build())
                        .build()));
    }
}
