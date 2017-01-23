package io.token;

import static io.token.asserts.TransferAssertion.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.proto.PagedList;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transfer.TransferProtos.Transfer;

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
        Token token = token();
        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();

        Transfer transfer = payee.redeemToken(token);
        assertThat(transfer)
                .hasNoAmount()
                .hasNSignatures(2)
                .isSignedBy(payee, Key.Level.LOW);
    }

    @Test
    public void redeemToken_withParams() {
        Token token = token();
        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();

        Transfer transfer = payee.redeemToken(token, 99.0, "USD", "transfer description");
        assertThat(transfer)
                .hasAmount(99.0)
                .hasCurrency("USD")
                .hasNSignatures(2)
                .isSignedBy(payee, Key.Level.LOW);
    }

    @Test
    public void getTransfer() {
        Token token = token();
        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();

        Transfer transfer = payee.redeemToken(token);
        Transfer lookedUp = payer.getTransfer(transfer.getId());
        assertThat(lookedUp).isEqualTo(transfer);
    }

    @Test
    public void getTransfers() {
        Token token = token();
        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();

        Transfer transfer1 = payee.redeemToken(token, 10.0, "USD", "first");
        Transfer transfer2 = payee.redeemToken(token, 20.0, "USD", "second");
        Transfer transfer3 = payee.redeemToken(token, 70.0, "USD", "third");

        assertThat(transfer1)
                .hasAmount(10.0)
                .hasCurrency("USD")
                .hasDescription("first");
        assertThat(transfer2)
                .hasAmount(20.0)
                .hasCurrency("USD")
                .hasDescription("second");
        assertThat(transfer3)
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

    private Token token() {
        return payer.createToken(
                100.0,
                "USD",
                payerAccount.id(),
                payee.firstUsername(),
                "book purchase");
    }
}
