package io.token;

import static io.token.asserts.TransferAssertion.assertThat;
import static io.token.common.Polling.waitUntil;
import static io.token.proto.TransactionStatusHelper.hasFailed;
import static io.token.proto.common.transaction.TransactionProtos.TransactionStatus.ACCEPTED;
import static io.token.proto.common.transaction.TransactionProtos.TransactionStatus.FAILURE_INSUFFICIENT_FUNDS;
import static io.token.proto.common.transaction.TransactionProtos.TransactionStatus.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.bank.TestAccount;
import io.token.common.LinkedAccount;
import io.token.common.TokenRule;
import io.token.proto.PagedList;
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

    private LinkedAccount payerAccount;
    private LinkedAccount payeeAccount;
    private Member payer;
    private Member payee;

    @Before
    public void before() {
        this.payerAccount = rule.linkedAccount();
        this.payeeAccount = rule.linkedAccount();
        this.payer = payerAccount.getMember();
        this.payee = payeeAccount.getMember();
    }

    @Test
    public void instantPayment_isSuccessful() {
        Transfer transfer = initiateInstantTransfer(100);
        assertThat(transfer).hasStatus(ACCEPTED);

        waitUntil(PAYMENT_CLEARING_TIMEOUT_MS, PAYMENT_CLEARING_POLL_FREQUENCY_MS, () -> {
            Transaction payerTransaction = payer.getTransaction(
                    payerAccount.getId(),
                    transfer.getReferenceId());

            Token token = payer.getToken(transfer.getPayload().getTokenId());
            Pricing pricing = token.getPayload().getTransfer().getPricing();

            assertThat(payerTransaction.getStatus())
                    .as("Payer Transaction Status")
                    .isEqualTo(SUCCESS);

            assertThat(pricing.getSourceQuote().getId()).as("Source Quote").isNotEmpty();
            assertThat(pricing.getDestinationQuote().getId())
                    .as("Destination Quote")
                    .isNotEmpty();
        });

        PagedList<Transaction, String> result = payerAccount.getAccount().getTransactions(null, 5);
        assertThat(result.getList()).isNotEmpty();
        assertThat(result.getOffset()).isNotEmpty();
    }

    @Test
    public void instantPayment_debitFailure() {
        double amount = payerAccount.getBalance() + 1;
        Transfer transfer = initiateInstantTransfer(amount);
        assertThat(transfer.getStatus()).isEqualTo(FAILURE_INSUFFICIENT_FUNDS);
    }

    @Ignore("PR-751")
    @Test
    public void instantPayment_adjustsAccountBalances() {
        final double transferAmount = 100.0;
        final double expectedPayerBalance = payerAccount.getBalance() - transferAmount;
        final double expectedPayeeBalance = payeeAccount.getBalance() + transferAmount;

        initiateInstantTransfer(transferAmount);

        waitUntil(PAYMENT_CLEARING_TIMEOUT_MS, PAYMENT_CLEARING_POLL_FREQUENCY_MS, () -> {
            assertThat(payerAccount.getBalance()).isEqualTo(expectedPayerBalance);
            assertThat(payeeAccount.getBalance()).isEqualTo(expectedPayeeBalance);
        });
    }

    @Ignore("PR-751")
    @Test
    public void instantPayment_rollback() {
        Token token = payer.createTransferToken(100.0, payeeAccount.getCurrency())
                .setAccountId(payerAccount.getId())
                .setRedeemerUsername(payee.firstUsername())
                .execute();
        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();

        TransferEndpoint transferEndpoint = TransferEndpoint.newBuilder()
                .setAccount(BankAccount.newBuilder()
                        .setToken(BankAccount.Token.newBuilder()
                                .setAccountId(payeeAccount.getId() + "invalidId")
                                .setMemberId(payee.memberId())))
                .build();

        final Transfer transfer = payee.redeemToken(
                token,
                100.00,
                payeeAccount.getCurrency(),
                transferEndpoint);

        waitUntil(PAYMENT_CLEARING_TIMEOUT_MS, PAYMENT_CLEARING_POLL_FREQUENCY_MS, () -> {
            Transaction transaction = payer.getTransaction(
                    payerAccount.getId(),
                    transfer.getReferenceId());

            assertThat(hasFailed(transaction.getStatus()));
        });
    }

    @Test
    public void instantPayment_nonTokenTipsDestination() {
        Transfer transfer = initiateInstantTransfer(rule.unlinkedAccount(),100);

        assertThat(transfer).hasStatus(ACCEPTED);

        waitUntil(PAYMENT_CLEARING_TIMEOUT_MS, PAYMENT_CLEARING_POLL_FREQUENCY_MS, () -> {
            Transaction payerTransaction = payer.getTransaction(
                    payerAccount.getId(),
                    transfer.getReferenceId());

            Pricing pricing = payer.getToken(transfer.getPayload().getTokenId())
                    .getPayload()
                    .getTransfer()
                    .getPricing();

            assertThat(payerTransaction.getStatus())
                    .as("Payer Transaction Status")
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
        TestAccount destination = rule.unlinkedAccount();

        final double expectedPayerBalance = payerAccount.getBalance() - transferAmount;
        final double expectedPayeeBalance = payeeAccount.getBalance() + transferAmount;

        initiateInstantTransfer(destination, transferAmount);

        waitUntil(PAYMENT_CLEARING_TIMEOUT_MS, PAYMENT_CLEARING_POLL_FREQUENCY_MS, () -> {
            assertThat(payerAccount.getBalance()).isEqualTo(expectedPayerBalance);
            assertThat(payeeAccount.getBalance()).isEqualTo(expectedPayeeBalance);
        });
    }

    private Transfer initiateInstantTransfer(double amount) {
        return initiateInstantTransfer(
                TransferEndpoint.newBuilder()
                        .setAccount(BankAccount.newBuilder()
                                .setToken(BankAccount.Token.newBuilder()
                                        .setAccountId(payeeAccount.getId())
                                        .setMemberId(payee.memberId())))
                        .build(),
                amount,
                payeeAccount.getCurrency());
    }

    private Transfer initiateInstantTransfer(TestAccount destination, double amount) {
        return initiateInstantTransfer(
                TransferEndpoint.newBuilder()
                        .setAccount(destination.getBankAccount())
                        .build(),
                amount,
                destination.getCurrency());
    }

    private Transfer initiateInstantTransfer(
            TransferEndpoint destination,
            double amount,
            String currency) {
        Token token = payer.createTransferToken(amount, currency)
                .setAccountId(payerAccount.getId())
                .setRedeemerUsername(payee.firstUsername())
                .addDestination(destination)
                .execute();
        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();
        return payee.redeemToken(token, amount, currency, "description");
    }
}
