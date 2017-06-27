package io.token;

import static io.grpc.Status.Code.FAILED_PRECONDITION;
import static io.token.asserts.TransferAssertion.assertThat;
import static io.token.proto.common.token.TokenProtos.TransferTokenStatus.FAILURE_INSUFFICIENT_FUNDS;
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
        this.payeeAccount = rule.linkedAccount(payerAccount);
        this.payee = payeeAccount.getMember();
    }

    @Test
    public void redeemToken_instant() {
        Token token = payerAccount
                .createInstantToken(100.0, payeeAccount)
                .execute();
        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();

        Transfer transfer = payee.redeemToken(token);
        assertThat(transfer)
                .isProcessing()
                .hasNoAmount()
                .hasNSignatures(2)
                .isSignedBy(payee, Key.Level.LOW);
    }

    @Test
    public void redeemToken_legacy() {
        Token token = payerAccount
                .createLegacyToken(100, payeeAccount)
                .execute();
        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();

        Transfer transfer = payee.redeemToken(token);
        assertThat(transfer)
                .isSuccessful()
                .hasNoAmount()
                .hasNSignatures(2)
                .isSignedBy(payee, Key.Level.LOW);
    }

    @Test
    public void redeemToken_defaultDestinationAccount() {
        Token token = payerAccount.createInstantToken(100, payeeAccount)
                .setRedeemerUsername(payee.firstUsername())
                .addDestination(Destinations.token(payee.memberId()))
                .execute();
        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();

        Transfer transfer = payee.redeemToken(token);
        assertThat(transfer)
                .isProcessing()
                .hasNoAmount()
                .hasNSignatures(2)
                .isSignedBy(payee, Key.Level.LOW);
    }

    @Test
    public void redeemToken_idempotentRefId() {
        Token token1 = instantToken(100);
        Token token2 = instantToken(100);

        String transferRefId = string();
        Transfer transfer1 = payee.redeemToken(token1, transferRefId);
        Transfer transfer2 = payee.redeemToken(token2, transferRefId);

        assertThat(transfer1).isEqualTo(transfer2);
    }

    @Test
    public void redeemTokenWithUnlinkedAccount() {
        Token token = payerAccount
                .createInstantToken(100.0, payeeAccount)
                .execute();
        final Token endorsedToken = payer.endorseToken(token, Key.Level.STANDARD).getToken();
        payer.unlinkAccounts(singletonList(payerAccount.getId()));
        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(() -> payee.redeemToken(endorsedToken))
                .matches(e -> e.getStatus().getCode() == FAILED_PRECONDITION);
    }

    @Test
    public void redeemToken_withParams() {
        Token token = instantToken(100);
        String refId = string();

        Transfer transfer = payee.redeemToken(
                token,
                99.0,
                payeeAccount.getCurrency(),
                "transfer description",
                null,
                refId);
        assertThat(transfer)
                .isProcessing()
                .hasAmount(99.0)
                .hasCurrency(payeeAccount.getCurrency())
                .hasRefId(refId)
                .hasNSignatures(2)
                .isSignedBy(payee, Key.Level.LOW);
    }

    @Test
    public void redeemToken_instant_failure() {
        double amount = payerAccount.getBalance() + 1; // 1 over the limit.

        assertThatExceptionOfType(TransferTokenException.class)
                .isThrownBy(() -> payerAccount
                        .createInstantToken(amount, payeeAccount)
                        .execute())
                .matches(e -> e.getStatus() == FAILURE_INSUFFICIENT_FUNDS);
    }

    @Test
    public void redeemToken_legacy_failure() {
        double amount = payerAccount.getBalance() + 1; // 1 over the limit.

        Token token = payerAccount
                .createLegacyToken(amount, payeeAccount)
                .execute();
        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();

        Transfer transfer = payee.redeemToken(
                token,
                amount,
                payeeAccount.getCurrency(),
                "transfer description");
        assertThat(transfer)
                .hasStatus(TransactionStatus.FAILURE_INSUFFICIENT_FUNDS)
                .hasAmount(amount)
                .hasCurrency(payeeAccount.getCurrency())
                .hasNSignatures(2)
                .isSignedBy(payee, Key.Level.LOW);
    }

    @Test
    public void getTransfer() {
        Token token = instantToken(100);

        Transfer transfer = payee.redeemToken(token);
        Transfer lookedUp = payer.getTransfer(transfer.getId());
        assertThat(lookedUp)
                .isProcessing()
                .isEqualTo(transfer);
    }

    @Test
    public void getTransfers_by_token() {
        Token token = instantToken(100);

        Transfer transfer1 = payee.redeemToken(
                token,
                10.0,
                payeeAccount.getCurrency(),
                "first");
        Transfer transfer2 = payee.redeemToken(
                token,
                20.0,
                payeeAccount.getCurrency(),
                "second");
        Transfer transfer3 = payee.redeemToken(
                token,
                70.0,
                payeeAccount.getCurrency(),
                "third");

        assertThat(transfer1)
                .isProcessing()
                .hasAmount(10.0)
                .hasCurrency(payeeAccount.getCurrency())
                .hasDescription("first");
        assertThat(transfer2)
                .isProcessing()
                .hasAmount(20.0)
                .hasCurrency(payeeAccount.getCurrency())
                .hasDescription("second");
        assertThat(transfer3)
                .isProcessing()
                .hasAmount(70.0)
                .hasCurrency(payeeAccount.getCurrency())
                .hasDescription("third");

        PagedList<Transfer, String> lookedUp = payer.getTransfers(null, 100, token.getId());
        assertThat(lookedUp.getList()).containsOnly(transfer1, transfer2, transfer3);
        assertThat(lookedUp.getOffset()).isNotEmpty();

        // Make sure the same results are returned if no token ID was provided
        lookedUp = payer.getTransfers(null, 100, null);
        assertThat(lookedUp.getList()).containsOnly(transfer1, transfer2, transfer3);
        assertThat(lookedUp.getOffset()).isNotEmpty();
    }

    @Test
    public void getTransfers_all_tokens() {
        Transfer transfer1 = payee.redeemToken(
                instantToken(10.0),
                10.0,
                payeeAccount.getCurrency(),
                "first");
        Transfer transfer2 = payee.redeemToken(
                instantToken(20.0),
                20.0,
                payeeAccount.getCurrency(),
                "second");
        Transfer transfer3 = payee.redeemToken(
                instantToken(70.0),
                70.0,
                payeeAccount.getCurrency(),
                "third");

        assertThat(transfer1)
                .isProcessing()
                .hasAmount(10.0)
                .hasCurrency(payeeAccount.getCurrency())
                .hasDescription("first");
        assertThat(transfer2)
                .isProcessing()
                .hasAmount(20.0)
                .hasCurrency(payeeAccount.getCurrency())
                .hasDescription("second");
        assertThat(transfer3)
                .isProcessing()
                .hasAmount(70.0)
                .hasCurrency(payeeAccount.getCurrency())
                .hasDescription("third");

        PagedList<Transfer, String> lookedUp = payer.getTransfers(null, 100, null);
        assertThat(lookedUp.getList()).containsOnly(transfer1, transfer2, transfer3);
        assertThat(lookedUp.getOffset()).isNotEmpty();
    }

    private Token instantToken(double amount) {
        Token token = payerAccount
                .createInstantToken(amount, payeeAccount)
                .execute();
        return payer.endorseToken(token, Key.Level.STANDARD).getToken();
    }
}
