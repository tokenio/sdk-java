package io.token;

import static io.token.asserts.TokenAssertion.assertThat;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableSet;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.token.common.TokenRule;
import io.token.proto.PagedList;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;

import org.assertj.core.api.ThrowableAssert;
import org.junit.Rule;
import org.junit.Test;

public class TransferTokenTest {
    @Rule public TokenRule rule = new TokenRule();
    private final Account payerAccount = rule.account();
    private final Account payeeAccount = rule.account();
    private final Member payer = payerAccount.member();
    private final Member payee = payeeAccount.member();

    @Test
    public void createTransferToken() {
        Token token = payer.createTransferToken(100.0, "USD")
                .setAccountId(payerAccount.id())
                .setRedeemerUsername(payee.firstUsername())
                .setDescription("book purchase")
                .execute();

        assertThat(token)
                .hasFrom(payer)
                .hasRedeemerUsername(payee.firstUsername())
                .hasAmount(100.0)
                .hasCurrency("USD")
                .hasNoSignatures();
    }

    @Test
    public void createTransferTokenWithUnlinkedAccount() {
        payer.unlinkAccounts(singletonList(payerAccount.id()));
        assertThatThrownBy(
                new ThrowableAssert.ThrowingCallable() {
                    public void call() throws Throwable {
                        payer.createTransferToken(100.0, "USD")
                                .setRedeemerUsername(payee.firstUsername())
                                .setAccountId(payerAccount.id())
                                .execute();
                    }
                })
                .isInstanceOf(StatusRuntimeException.class)
                .extracting("status")
                .extracting("code", Status.Code.class)
                .extractingResultOf("value", int.class)
                .contains(Status.Code.FAILED_PRECONDITION.value());
    }

    @Test
    public void getTransferToken() {
        Token token = payer.createTransferToken(100.0, "USD")
                .setAccountId(payerAccount.id())
                .setRedeemerUsername(payee.firstUsername())
                .execute();
        assertThat(payer.getToken(token.getId()))
                .hasFrom(payer)
                .hasAmount(100.0)
                .hasCurrency("USD")
                .hasNoSignatures();
    }

    @Test
    public void getTransferTokens() {
        Token token1 = payer.createTransferToken(123.45, "EUR")
                .setAccountId(payerAccount.id())
                .setRedeemerUsername(payee.firstUsername())
                .execute();
        Token token2 = payer.createTransferToken(678.90, "USD")
                .setAccountId(payerAccount.id())
                .setRedeemerUsername(payee.firstUsername())
                .execute();
        Token token3 = payer.createTransferToken(100.99, "USD")
                .setAccountId(payerAccount.id())
                .setRedeemerUsername(payee.firstUsername())
                .execute();

        PagedList<Token, String> res = payer.getTransferTokens(null, 3);
        assertThat(res.getList()).containsOnly(token1, token2, token3);
        assertThat(res.getOffset()).isNotEmpty();
    }

    @Test
    public void getTransferTokensPaged() {
        int num = 10;
        for (int i = 0; i < num; i++) {
            Token token = payer.createTransferToken(100 + i, "EUR")
                    .setAccountId(payerAccount.id())
                    .setRedeemerUsername(payee.firstUsername())
                    .execute();
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
        Token token = payer.createTransferToken(100.0, "USD")
                .setAccountId(payerAccount.id())
                .setRedeemerUsername(payee.firstUsername())
                .execute();
        TokenOperationResult result = payer.endorseToken(token, Key.Level.STANDARD);
        assertThat(result.getStatus())
                .isEqualTo(TokenOperationResult.Status.SUCCESS);
        assertThat(result.getToken())
                .hasNSignatures(2)
                .isEndorsedBy(payer, Key.Level.STANDARD)
                .hasFrom(payer)
                .hasAmount(100.0)
                .hasCurrency("USD");
    }

    @Test
    public void endorseTransferTokenWithUnlinkedAccount() {
        final Token token = payer.createTransferToken(100.0, "USD")
                .setAccountId(payerAccount.id())
                .setRedeemerUsername(payee.firstUsername())
                .execute();
        payer.unlinkAccounts(singletonList(payerAccount.id()));
        assertThatThrownBy(
                new ThrowableAssert.ThrowingCallable() {
                    public void call() throws Throwable {
                        payer.endorseToken(token, Key.Level.STANDARD);
                    }
                })
                .isInstanceOf(StatusRuntimeException.class)
                .extracting("status")
                .extracting("code", Status.Code.class)
                .extractingResultOf("value", int.class)
                .contains(Status.Code.FAILED_PRECONDITION.value());
    }

    @Test
    public void cancelTransferToken() {
        Token token = payer.createTransferToken(100.0, "USD")
                .setAccountId(payerAccount.id())
                .setRedeemerUsername(payee.firstUsername())
                .execute();
        TokenOperationResult result = payer.cancelToken(token);
        assertThat(result.getStatus())
                .isEqualTo(TokenOperationResult.Status.SUCCESS);

        assertThat(result.getToken())
                .hasNSignatures(2)
                .isCancelledBy(payer, Key.Level.LOW)
                .hasFrom(payer)
                .hasAmount(100.0)
                .hasCurrency("USD");
    }

    @Test
    public void endorseTransferTokenMoreSignaturesNeeded() {
        Token token = payer.createTransferToken(100.0, "USD")
                .setAccountId(payerAccount.id())
                .setRedeemerUsername(payee.firstUsername())
                .execute();
        TokenOperationResult result = payer.endorseToken(token, Key.Level.LOW);

        assertThat(result.getStatus())
                .isEqualTo(TokenOperationResult.Status.MORE_SIGNATURES_NEEDED);
        assertThat(result.getToken())
                .hasNSignatures(1)
                .isEndorsedBy(payer, Key.Level.LOW)
                .hasFrom(payer)
                .hasAmount(100.0)
                .hasCurrency("USD");
    }
}
