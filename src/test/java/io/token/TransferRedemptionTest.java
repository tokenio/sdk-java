package io.token;

import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static io.token.asserts.TransferAssertion.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

public class TransferRedemptionTest {
    @Rule public TokenRule rule = new TokenRule();
    private final Account payerAccount = rule.account();
    private final Account payeeAccount = rule.account();
    private final Member payer = payerAccount.member();
    private final Member payee = payeeAccount.member();

    @Test
    public void redeemToken() {
        Token token = payer.createTransferToken(
                100.0,
                "USD",
                payerAccount.id(),
                payee.firstAlias(),
                "book purchase");
        token = payer.endorseTransferToken(token);

        Transfer transfer = payee.redeemTransferToken(token);
        assertThat(transfer)
                .hasAmount(100.0)
                .hasCurrency("USD")
                .hasNSignatures(2)
                .isSignedBy(payee);
    }

    @Test
    public void redeemToken_withParams() {
        Token token = payer.createTransferToken(
                100.0,
                "USD",
                payerAccount.id(),
                payee.firstAlias(),
                "book purchase");
        token = payer.endorseTransferToken(token);

        Transfer transfer = payee.redeemTransferToken(token, 99.0, "USD");
        assertThat(transfer)
                .hasAmount(99.0)
                .hasCurrency("USD")
                .hasNSignatures(2)
                .isSignedBy(payee);
    }

    @Test
    public void getTransfer() {
        Token token = payer.createTransferToken(
                100.0,
                "USD",
                payerAccount.id(),
                payee.firstAlias(),
                "book purchase");
        token = payer.endorseTransferToken(token);

        Transfer transfer = payee.redeemTransferToken(token);
        Transfer lookedUp = payer.getTransfer(transfer.getId());
        assertThat(lookedUp).isEqualTo(transfer);
    }

    @Test
    public void getTransfers() {
        Token token = payer.createTransferToken(
                100.0,
                "USD",
                payerAccount.id(),
                payee.firstAlias(),
                "book purchase");
        token = payer.endorseTransferToken(token);

        Transfer transfer1 = payee.redeemTransferToken(token, 10.0, "USD");
        Transfer transfer2 = payee.redeemTransferToken(token, 20.0, "USD");
        Transfer transfer3 = payee.redeemTransferToken(token, 70.0, "USD");

        assertThat(transfer1)
                .hasAmount(10.0)
                .hasCurrency("USD");
        assertThat(transfer2)
                .hasAmount(20.0)
                .hasCurrency("USD");
        assertThat(transfer3)
                .hasAmount(70.0)
                .hasCurrency("USD");

        List<Transfer> lookedUp = payer.getTransfers(0, 100, token.getId());
        assertThat(lookedUp).containsOnly(transfer1, transfer2, transfer3);
    }
}
