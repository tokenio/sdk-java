package io.token;

import static io.token.asserts.TransferAssertion.assertThat;
import static io.token.proto.common.security.SecurityProtos.SealedMessage.MethodCase.NOOP;
import static java.util.Collections.singletonList;

import io.token.common.TokenRule;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.sdk.BankAccount;
import io.token.sdk.BankAccountAuthorizer;
import io.token.security.testing.KeyStoreTestRule;

import org.junit.Rule;
import org.junit.Test;

public class BankAuthorizationPayTest {
    @Rule public TokenRule rule = new TokenRule();
    @Rule public KeyStoreTestRule keyRule = new KeyStoreTestRule();
    private final Account payerAccount = rule.account();
    private final Account payeeAccount = rule.account();
    private final Member payer = payerAccount.member();
    private final Member payee = payeeAccount.member();

    @Test
    public void redeemToken() {
        Token token = token(100.0);
        token = payer.endorseToken(token, Key.Level.STANDARD).getToken();

        Transfer transfer = payer.redeemToken(token);
        assertThat(transfer)
                .isSuccessful()
                .hasNoAmount()
                .hasNSignatures(2)
                .isSignedBy(payer, Key.Level.LOW);
    }

    private Token token(double amount) {
        BankAccount account = rule.unlinkedAccount();
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
