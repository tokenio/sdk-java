package io.token;

import static io.token.asserts.TransferAssertion.assertThat;
import static io.token.common.Polling.waitUntil;
import static io.token.proto.TransactionStatusHelper.hasFailed;
import static io.token.proto.common.transaction.TransactionProtos.TransactionStatus.ACCEPTED;
import static io.token.proto.common.transaction.TransactionProtos.TransactionStatus.FAILURE_INSUFFICIENT_FUNDS;
import static io.token.proto.common.transaction.TransactionProtos.TransactionStatus.PROCESSING;
import static io.token.proto.common.transaction.TransactionProtos.TransactionStatus.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.common.TokenRule;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.pricing.PricingProtos.Pricing;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferEndpoint;

import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class InstantPaymentTest {
    private static final int PAYMENT_CLEARING_TIMEOUT_MS = 90000;
    private static final int PAYMENT_CLEARING_POLL_FREQUENCY_MS = 1000;

    @Rule public TokenRule rule = new TokenRule();

    private Account payerAccount;
    private Account payeeAccount;
    private Member payer;
    private Member payee;

    @Before
    public void before() {
        this.payerAccount = rule.account();
        this.payeeAccount = rule.account();
        this.payer = payerAccount.member();
        this.payee = payeeAccount.member();
    }

    @Test
    public void instantPayment_isSuccessful() {
        final Transfer transfer = initiateInstantTransfer(100);

        assertThat(transfer).hasStatus(ACCEPTED);

        waitUntil(PAYMENT_CLEARING_TIMEOUT_MS, PAYMENT_CLEARING_POLL_FREQUENCY_MS, () -> {
            Transaction payerTransaction = payer.getTransaction(
                    payerAccount.id(),
                    transfer.getReferenceId());

            Transaction payeeTransaction = payee.getTransaction(
                    payeeAccount.id(),
                    transfer.getReferenceId());

            Token token = payer.getToken(transfer.getPayload().getTokenId());
            Pricing pricing = token.getPayload().getTransfer().getPricing();

            assertThat(payerTransaction.getStatus())
                    .as("Payer Transaction Status")
                    .isEqualTo(SUCCESS);
            assertThat(payeeTransaction.getStatus())
                    .as("Payee Transaction Status")
                    .isEqualTo(SUCCESS);

            assertThat(pricing.getSourceQuote().getId()).as("Source Quote").isNotEmpty();
            assertThat(pricing.getDestinationQuote().getId())
                    .as("Destination Quote")
                    .isNotEmpty();
        });
    }

    @Test
    public void instantPayment_debitFailure() {
        double amount = Double.parseDouble(payerAccount.getBalance().getValue()) + 1;

        Transfer transfer = initiateInstantTransfer(amount);

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

        waitUntil(PAYMENT_CLEARING_TIMEOUT_MS, PAYMENT_CLEARING_POLL_FREQUENCY_MS, () -> {
            double actualPayerBalance =
                    Double.parseDouble(payerAccount.getBalance().getValue());
            double actualPayeeBalance =
                    Double.parseDouble(payeeAccount.getBalance().getValue());

            assertThat(actualPayerBalance).isEqualTo(expectedPayerBalance);
            assertThat(actualPayeeBalance).isEqualTo(expectedPayeeBalance);
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

        waitUntil(PAYMENT_CLEARING_TIMEOUT_MS, PAYMENT_CLEARING_POLL_FREQUENCY_MS, () -> {
            Transaction transaction = payer.getTransaction(
                    payerAccount.id(),
                    transfer.getReferenceId());

            assertThat(hasFailed(transaction.getStatus()));
        });
    }

    @Test
    public void instantPayment_nonTokenTipsDestination() {
        Transfer transfer = initiateInstantTransfer(100, TransferEndpoint.newBuilder()
                .setAccount(rule.bankAccount(payee))
                .build());

        assertThat(transfer).hasStatus(ACCEPTED);

        waitUntil(PAYMENT_CLEARING_TIMEOUT_MS, PAYMENT_CLEARING_POLL_FREQUENCY_MS, () -> {
            Transaction payerTransaction = payer.getTransaction(
                    payerAccount.id(),
                    transfer.getReferenceId());

            Transaction payeeTransaction = payee.getTransaction(
                    payeeAccount.id(),
                    transfer.getReferenceId());

            Pricing pricing = payer.getToken(transfer.getPayload().getTokenId())
                    .getPayload()
                    .getTransfer()
                    .getPricing();

            assertThat(payerTransaction.getStatus())
                    .as("Payer Transaction Status")
                    .isEqualTo(SUCCESS);

            assertThat(payeeTransaction.getStatus())
                    .as("Payee Transaction Status")
                    .isEqualTo(SUCCESS);

            assertThat(pricing.getSourceQuote().getId()).as("Source Quote").isNotEmpty();

            assertThat(pricing.getDestinationQuote().getId())
                    .as("Destination Quote")
                    .isNotEmpty();
        });
    }

    @Ignore("PR-751")
    @Test
    public void instantPayment_nonTokenTipsDestinationAdjustsAccountBalances() {
        final double transferAmount = 100.0;

        payee.unlinkAccounts(payee.getAccounts()
                .stream()
                .map(Account::id)
                .collect(Collectors.toList()));
        BankAccount bankAccount = rule.bankAccount(payee);
        Account merchantAccount = payee.getAccounts().get(0);

        final double expectedPayerBalance =
                Double.parseDouble(payerAccount.getBalance().getValue()) - transferAmount;
        final double expectedPayeeBalance =
                Double.parseDouble(merchantAccount.getBalance().getValue()) + transferAmount;

        initiateInstantTransfer(transferAmount, TransferEndpoint.newBuilder()
                .setAccount(bankAccount)
                .build());

        waitUntil(PAYMENT_CLEARING_TIMEOUT_MS, PAYMENT_CLEARING_POLL_FREQUENCY_MS, () -> {
            double actualPayerBalance =
                    Double.parseDouble(payerAccount.getBalance().getValue());
            double actualPayeeBalance =
                    Double.parseDouble(merchantAccount.getBalance().getValue());

            assertThat(actualPayerBalance).isEqualTo(expectedPayerBalance);
            assertThat(actualPayeeBalance).isEqualTo(expectedPayeeBalance);
        });
    }

    private Transfer initiateInstantTransfer(double amount) {
        return initiateInstantTransfer(amount, TransferEndpoint.newBuilder()
                .setAccount(BankAccount.newBuilder()
                        .setToken(BankAccount.Token.newBuilder()
                                .setAccountId(payeeAccount.id())
                                .setMemberId(payee.memberId())))
                .build());
    }

    private Transfer initiateInstantTransfer(double amount, TransferEndpoint destination) {
        Token token = payer.createTransferToken(amount, "USD")
                .setAccountId(payerAccount.id())
                .setRedeemerUsername(payee.firstUsername())
                .addDestination(destination)
                .execute();

        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();

        return payee.redeemToken(token, amount, "USD", "description");
    }
}
