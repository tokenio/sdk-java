package io.token;

import static io.token.asserts.TransferAssertion.assertThat;
import static io.token.proto.TransactionStatusHelper.hasFailed;
import static io.token.proto.common.transaction.TransactionProtos.TransactionStatus.FAILURE_INSUFFICIENT_FUNDS;
import static io.token.proto.common.transaction.TransactionProtos.TransactionStatus.PROCESSING;
import static io.token.proto.common.transaction.TransactionProtos.TransactionStatus.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.util.concurrent.Uninterruptibles;
import io.token.common.TokenRule;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferEndpoint;

import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class InstantPaymentTest {
    private static final int TIMEOUT_MS = 90000;

    @Rule public TokenRule rule = new TokenRule();
    private final Account payerAccount = rule.account();
    private final Account payeeAccount = rule.account();
    private final Member payer = payerAccount.member();
    private final Member payee = payeeAccount.member();

    @Test
    public void instantPayment_isSuccessful() {
        final Transfer transfer = initiateInstantTransfer(100);

        assertThat(transfer).hasStatus(PROCESSING);

        waitUntil(new Runnable() {
            @Override
            public void run() {
                Transaction transaction = payer.getTransaction(
                        payerAccount.id(),
                        transfer.getReferenceId());

                assertThat(transaction.getStatus()).isEqualTo(SUCCESS);
            }
        });
    }

    @Test
    public void instantPayment_debitFailure() {
        double amount = Double.parseDouble(payerAccount.getBalance().getValue()) + 1;

        final Transfer transfer = initiateInstantTransfer(amount);

        assertThat(transfer.getStatus()).isEqualTo(FAILURE_INSUFFICIENT_FUNDS);
    }

    @Ignore("PR-751")
    @Test
    public void instantPayment_adjustsAccountBalances() {
        final double transferAmount = 100.0;
        final double expectedPayerBalance =
                Double.parseDouble(payerAccount.getBalance().getValue()) - transferAmount;
        final double expectedPayeeBalance =
                Double.parseDouble(payeeAccount.getBalance().getValue()) + transferAmount;

        initiateInstantTransfer(transferAmount);

        waitUntil(new Runnable() {
            @Override
            public void run() {
                double actualPayerBalance =
                        Double.parseDouble(payerAccount.getBalance().getValue());
                double actualPayeeBalance =
                        Double.parseDouble(payeeAccount.getBalance().getValue());

                assertThat(actualPayerBalance).isEqualTo(expectedPayerBalance);
                assertThat(actualPayeeBalance).isEqualTo(expectedPayeeBalance);
            }
        });
    }

    @Ignore("PR-751")
    @Test
    public void instantPayment_rollback() {
        Token token = payer.createTransferToken(100.0, "USD")
                .setAccountId(payerAccount.id())
                .setRedeemerUsername(payee.firstUsername())
                .execute();
        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();

        TransferEndpoint transferEndpoint = TransferEndpoint.newBuilder()
                .setAccount(BankAccount.newBuilder()
                        .setToken(BankAccount.Token.newBuilder()
                                .setAccountId(payeeAccount.id() + "invalidId")
                                .setMemberId(payee.memberId())))
                .build();

        final Transfer transfer = payee.redeemToken(token, 100.00, "USD", transferEndpoint);

        waitUntil(new Runnable() {
            @Override
            public void run() {
                Transaction transaction = payer.getTransaction(
                        payerAccount.id(),
                        transfer.getReferenceId());

                assertThat(hasFailed(transaction.getStatus()));
            }
        });
    }

    private Transfer initiateInstantTransfer(double amount) {
        Token token = payer.createTransferToken(amount, "USD")
                .setAccountId(payerAccount.id())
                .setRedeemerUsername(payee.firstUsername())
                .execute();

        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();

        TransferEndpoint transferEndpoint = TransferEndpoint.newBuilder()
                .setAccount(BankAccount.newBuilder()
                        .setToken(BankAccount.Token.newBuilder()
                                .setAccountId(payeeAccount.id())
                                .setMemberId(payee.memberId())))
                .build();

        return payee.redeemToken(token, amount, "USD", transferEndpoint);
    }

    private void waitUntil(Runnable function) {
        for (long waitTimeMs = 1000, start = System.currentTimeMillis(); ; ) {
            try {
                function.run();
                return;
            } catch (AssertionError caughtError) {
                if (System.currentTimeMillis() - start < TIMEOUT_MS) {
                    Uninterruptibles.sleepUninterruptibly(waitTimeMs, TimeUnit.MILLISECONDS);
                } else {
                    throw caughtError;
                }
            }
        }
    }
}
