package io.token;

import io.token.proto.PagedList;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import org.junit.Rule;
import org.junit.Test;

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
        Token token = token();
        token = payer.endorseToken(token).getToken();

        Transfer transfer = payee.redeemToken(token);
        assertThat(transfer)
                .hasNoAmount()
                .hasNSignatures(2)
                .isSignedBy(payee);
    }

    @Test
    public void redeemToken_withParams() {
        Token token = token();
        token = payer.endorseToken(token).getToken();

        Transfer transfer = payee.redeemToken(token, 99.0, "USD");
        assertThat(transfer)
                .hasAmount(99.0)
                .hasCurrency("USD")
                .hasNSignatures(2)
                .isSignedBy(payee);
    }

    @Test
    public void getTransfer() {
        Token token = token();
        token = payer.endorseToken(token).getToken();

        Transfer transfer = payee.redeemToken(token);
        Transfer lookedUp = payer.getTransfer(transfer.getId());
        assertThat(lookedUp).isEqualTo(transfer);
    }

    @Test
    public void getTransfers() {
        Token token = token();
        token = payer.endorseToken(token).getToken();

        Transfer transfer1 = payee.redeemToken(token, 10.0, "USD");
        Transfer transfer2 = payee.redeemToken(token, 20.0, "USD");
        Transfer transfer3 = payee.redeemToken(token, 70.0, "USD");

        assertThat(transfer1)
                .hasAmount(10.0)
                .hasCurrency("USD");
        assertThat(transfer2)
                .hasAmount(20.0)
                .hasCurrency("USD");
        assertThat(transfer3)
                .hasAmount(70.0)
                .hasCurrency("USD");

        PagedList<Transfer, String> lookedUp = payer.getTransfers(null, 100, token.getId());
        assertThat(lookedUp.getList()).containsOnly(transfer1, transfer2, transfer3);
        assertThat(lookedUp.getOffset()).isNotEmpty();

        // Make sure the same results are returned if no token ID was provided
        lookedUp = payer.getTransfers(null, 100, null);
        assertThat(lookedUp.getList()).containsOnly(transfer1, transfer2, transfer3);
        assertThat(lookedUp.getOffset()).isNotEmpty();
    }

    private Token token() {
        return payer.createToken(
                100.0,
                "USD",
                payerAccount.id(),
                payee.firstUsername(),
                "book purchase");
    }
}
