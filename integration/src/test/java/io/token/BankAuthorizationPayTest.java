package io.token;

import static io.token.asserts.TransferAssertion.assertThat;
import static io.token.proto.common.security.SecurityProtos.SealedMessage.MethodCase.NOOP;
import static java.util.Collections.singletonList;

import io.token.common.TokenRule;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.sdk.BankAccountAuthorizer;
import io.token.sdk.NamedAccount;
import io.token.security.testing.KeyStoreTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class BankAuthorizationPayTest {
    @Rule public TokenRule rule = new TokenRule();
    @Rule public KeyStoreTestRule keyRule = new KeyStoreTestRule();

    private Member payer;
    private Account payeeAccount;
    private Member payee;

    @Before
    public void before() {
        Account payerAccount = rule.account();
        this.payer = payerAccount.member();

        this.payeeAccount = rule.account();
        this.payee = payeeAccount.member();
    }

    @Test
    public void redeemToken() {
        Token token = token(100.0);
        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();

        Transfer transfer = payer.redeemToken(token);
        assertThat(transfer)
                .isAccepted()
                .hasNoAmount()
                .hasNSignatures(2)
                .isSignedBy(payer, Key.Level.LOW);
    }

    private Token token(double amount) {
        NamedAccount account = rule.unlinkedAccount();
        BankAccountAuthorizer authorizer = BankAccountAuthorizer
                .builder("iron")
                .useMethod(NOOP)
                .withSecretKeystore(keyRule.getSecretKeyStore())
                .withTrustedKeystore(keyRule.getTrustedKeyStore())
                .useKey(keyRule.getEncryptionKeyId())
                .build();

        BankAuthorization authorization = authorizer.createAuthorization(
                payer.firstUsername(),
                singletonList(account));

        return payer.createTransferToken(amount, "USD")
                .setBankAuthorization(authorization)
                .setRedeemerUsername(payer.firstUsername())
                .addDestination(Destinations.token(payee.memberId(), payeeAccount.id()))
                .execute();
    }
}
