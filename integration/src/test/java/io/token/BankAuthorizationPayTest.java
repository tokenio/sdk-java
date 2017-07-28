package io.token;

import static io.token.asserts.TransferAssertion.assertThat;
import static io.token.proto.common.security.SecurityProtos.SealedMessage.MethodCase.NOOP;
import static java.util.Collections.singletonList;

import io.token.common.LinkedAccount;
import io.token.common.TokenRule;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.sdk.BankAccountAuthorizer;
import io.token.security.testing.KeyStoreTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class BankAuthorizationPayTest {
    @Rule public TokenRule rule = new TokenRule();
    @Rule public KeyStoreTestRule keyRule = new KeyStoreTestRule();

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
    public void redeemToken() {
        Token token = token(100.0);
        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();

        Transfer transfer = payer.redeemToken(token);
        assertThat(transfer)
                .isProcessing()
                .hasNoAmount()
                .hasNSignatures(2)
                .isSignedBy(payer, Key.Level.LOW);
    }

    private Token token(double amount) {
        BankAccountAuthorizer authorizer = BankAccountAuthorizer
                .builder(rule.getBankId())
                .useMethod(NOOP)
                .withSecretKeystore(keyRule.getSecretKeyStore())
                .withTrustedKeystore(keyRule.getTrustedKeyStore())
                .useKey(keyRule.getEncryptionKeyId())
                .build();

        BankAuthorization authorization = authorizer.createAuthorization(
                payer.memberId(),
                singletonList(payerAccount.testAccount().toNamedAccount()));

        return payer.createTransferToken(amount, payeeAccount.getCurrency())
                .setBankAuthorization(authorization)
                .setRedeemerAlias(payer.firstAlias())
                .addDestination(Destinations.token(payee.memberId(), payeeAccount.getId()))
                .execute();
    }
}
