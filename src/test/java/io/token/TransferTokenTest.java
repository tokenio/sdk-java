package io.token;

import com.google.common.collect.ImmutableSet;
import io.token.proto.PagedList;
import io.token.proto.common.security.SecurityProtos;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.proto.common.token.TokenProtos.TokenOperationResult.Status;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.security.Crypto;
import io.token.security.SecretKey;
import org.junit.Rule;
import org.junit.Test;

import static io.token.asserts.TokenAssertion.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

public class TransferTokenTest {
    @Rule public TokenRule rule = new TokenRule();
    private final Account payerAccount = rule.account();
    private final Account payeeAccount = rule.account();
    private final Member payer = payerAccount.member();
    private final Member payee = payeeAccount.member();

    @Test
    public void createTransferToken() {
        Token token = payer.createToken(
                100.0,
                "USD",
                payerAccount.id(),
                payee.firstUsername(),
                "book purchase");

        assertThat(token)
                .hasFrom(payer)
                .hasRedeemerUsername(payee.firstUsername())
                .hasAmount(100.0)
                .hasCurrency("USD")
                .hasNoSignatures();
    }

    @Test
    public void getTransferToken() {
        Token token = payer.createToken(100.0, "USD", payerAccount.id());
        assertThat(payer.getToken(token.getId()))
                .hasFrom(payer)
                .hasAmount(100.0)
                .hasCurrency("USD")
                .hasNoSignatures();
    }

    @Test
    public void getTransferTokens() {
        Token token1 = payer.createToken(123.45, "EUR", payerAccount.id());
        Token token2 = payer.createToken(678.90, "USD", payerAccount.id());
        Token token3 = payer.createToken(100.99, "USD", payerAccount.id());

        PagedList<Token, String> res = payer.getTransferTokens(null, 3);
        assertThat(res.getList()).containsOnly(token1, token2, token3);
        assertThat(res.getOffset()).isNotEmpty();
    }

    @Test
    public void getTransferTokensPaged() {
        int num = 10;
        for (int i = 0; i < num; i++) {
            payer.createToken(100.0 + i, "EUR", payerAccount.id());
        }

        int limit = 2;
        ImmutableSet.Builder<Token> builder = ImmutableSet.builder();
        PagedList<Token, String> result = payer.getTransferTokens(null, limit);
        for (int i = 0; i < num / limit; i++) {
            builder.addAll(result.getList());
            result = payer.getTransferTokens(result.getOffset(), limit);
        }

        assertThat(builder.build().size()).isEqualTo(num);
    }

    @Test
    public void endorseTransferToken() {
        Token token = payer.createToken(100.0, "USD", payerAccount.id());
        TokenOperationResult result = payer.endorseToken(token);
        assertThat(result.getStatus()).isEqualTo(Status.SUCCESS);
        assertThat(result.getToken())
                .hasNSignatures(2)
                .isEndorsedBy(payer)
                .hasFrom(payer)
                .hasAmount(100.0)
                .hasCurrency("USD");
    }

    @Test
    public void cancelTransferToken() {
        Token token = payer.createToken(100.0, "USD", payerAccount.id());
        TokenOperationResult result = payer.cancelToken(token);
        assertThat(result.getStatus()).isEqualTo(Status.SUCCESS);

        assertThat(result.getToken())
                .hasNSignatures(2)
                .isCancelledBy(payer)
                .hasFrom(payer)
                .hasAmount(100.0)
                .hasCurrency("USD");
    }

    @Test
    public void endorseTransferTokenMoreSignaturesNeeded() {
        SecretKey key2 = Crypto.generateSecretKey();
        payer.approveKey(key2.getPublicKey(), SecurityProtos.Key.Level.LOW);
        Member low = rule.token().login(payer.memberId(), key2);

        Token token = payer.createToken(100.0, "USD", payerAccount.id());
        TokenOperationResult result = low.endorseToken(token);
        assertThat(result.getStatus()).isEqualTo(Status.MORE_SIGNATURES_NEEDED);
        assertThat(result.getToken())
                .hasNSignatures(2)
                .isEndorsedBy(payer)
                .hasFrom(payer)
                .hasAmount(100.0)
                .hasCurrency("USD");
    }
}
