package io.token;

import static io.token.asserts.TransferAssertion.assertThat;
import static io.token.common.Polling.waitUntil;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.proto.common.transaction.TransactionProtos.TransactionStatus.FAILURE_CANCELED;
import static io.token.proto.common.transaction.TransactionProtos.TransactionStatus.PROCESSING;
import static io.token.proto.common.transaction.TransactionProtos.TransactionStatus.SUCCESS;
import static java.lang.Double.parseDouble;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.token.bank.TestAccount;
import io.token.common.LinkedAccount;
import io.token.common.TokenRule;
import io.token.proto.PagedList;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.pricing.PricingProtos.Pricing;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TransferTokenStatus;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferEndpoint;

import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class InstantPaymentTest {
    private static final int PAYMENT_CLEARING_TIMEOUT_MS = 180000;
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
        assertThat(transfer).hasStatus(PROCESSING);

        waitUntil(PAYMENT_CLEARING_TIMEOUT_MS, PAYMENT_CLEARING_POLL_FREQUENCY_MS, () -> {
            Transaction payerTransaction = payer.getTransaction(
                    payerAccount.getId(),
                    transfer.getTransactionId());

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
    public void instantPayment_insufficientFunds() {
        double amount = payerAccount.getBalance() + 1;
        assertThatExceptionOfType(TransferTokenException.class)
                .isThrownBy(() -> payerAccount.createInstantToken(amount, payeeAccount)
                        .setRedeemerUsername(payee.firstUsername())
                        .execute())
                .matches(e -> e.getStatus() == TransferTokenStatus.FAILURE_INSUFFICIENT_FUNDS);
    }

    @Test
    public void instantPayment_adjustsAccountBalances() {
        double transferAmount = 100.0;
        double payerBalance = payerAccount.getBalance();
        double payeeBalance = payeeAccount.getBalance();

        Transfer transfer = initiateInstantTransfer(transferAmount);
        Token token = payer.getToken(transfer.getPayload().getTokenId());
        Pricing pricing = token.getPayload().getTransfer().getPricing();

        double payerFee = parseDouble(pricing.getSourceQuote().getFeesTotal());
        double payeeFee = parseDouble(pricing.getDestinationQuote().getFeesTotal());
        double expectedPayerBalance = payerBalance - transferAmount - payerFee;
        double expectedPayeeBalance = payeeBalance + transferAmount - payeeFee;

        waitUntil(PAYMENT_CLEARING_TIMEOUT_MS, PAYMENT_CLEARING_POLL_FREQUENCY_MS, () -> {
            double payerAccountBalance = payerAccount.getBalance();
            double payeeAccountBalance = payeeAccount.getBalance();
            assertThat(payerAccountBalance).isEqualTo(expectedPayerBalance);
            assertThat(payeeAccountBalance).isEqualTo(expectedPayeeBalance);
        });
    }

    @Test
    public void instantPayment_rollback() {
        LinkedAccount rejectAccount = rule.rejectLinkedAccount();
        Token token = payerAccount
                .createInstantToken(100.0, rejectAccount)
                .execute();
        token = payer.endorseToken(token, STANDARD).getToken();
        Transfer transfer = rejectAccount.getMember().redeemToken(token);

        waitUntil(PAYMENT_CLEARING_TIMEOUT_MS, PAYMENT_CLEARING_POLL_FREQUENCY_MS, () -> {
            Transaction transaction = payer.getTransaction(
                    payerAccount.getId(),
                    transfer.getTransactionId());
            assertThat(transaction.getStatus()).isEqualTo(FAILURE_CANCELED);
        });
    }

    @Test
    public void instantPayment_nonTokenTipsDestination() {
        Transfer transfer = initiateInstantTransfer(rule.unlinkedAccount(),100);

        assertThat(transfer).hasStatus(PROCESSING);

        waitUntil(PAYMENT_CLEARING_TIMEOUT_MS, PAYMENT_CLEARING_POLL_FREQUENCY_MS, () -> {
            Transaction payerTransaction = payer.getTransaction(
                    payerAccount.getId(),
                    transfer.getTransactionId());

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

    @Test
    public void instantPayment_nonTokenTipsDestinationAdjustsAccountBalances() {
        final double transferAmount = 100.0;
        final double payerBalance = payerAccount.getBalance();

        payee.unlinkAccounts(payee.getAccounts()
                .stream()
                .map(Account::id)
                .collect(Collectors.toList()));
        TestAccount destination = rule.unlinkedAccount();

        Transfer transfer = initiateInstantTransfer(destination, transferAmount);
        Token token = payer.getToken(transfer.getPayload().getTokenId());
        Pricing pricing = token.getPayload().getTransfer().getPricing();

        final double payerFee = parseDouble(pricing.getSourceQuote().getFeesTotal());
        final double expectedPayerBalance = payerBalance - transferAmount - payerFee;

        waitUntil(PAYMENT_CLEARING_TIMEOUT_MS, PAYMENT_CLEARING_POLL_FREQUENCY_MS, () -> {
            double payerAccountBalance = payerAccount.getBalance();
            assertThat(payerAccountBalance).isEqualTo(expectedPayerBalance);
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
        token = payer.endorseToken(token, STANDARD).getToken();
        return payee.redeemToken(token, amount, currency, "description");
    }
}
