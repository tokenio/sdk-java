package io.token;

import static io.grpc.Status.Code.FAILED_PRECONDITION;
import static io.token.asserts.TokenAssertion.assertThat;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.google.common.collect.ImmutableSet;
import io.grpc.StatusRuntimeException;
import io.token.common.LinkedAccount;
import io.token.common.TokenRule;
import io.token.proto.PagedList;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.proto.common.token.TokenProtos.TransferTokenStatus;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferEndpoint;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TransferTokenTest {
    @Rule public TokenRule rule = new TokenRule();

    private LinkedAccount payerAccount;
    private Member payer;
    private LinkedAccount payeeAccount;
    private Member payee;

    @Before
    public void before() {
        this.payerAccount = rule.linkedAccount();
        this.payer = payerAccount.getMember();

        this.payeeAccount = rule.linkedAccount();
        this.payee = payeeAccount.getMember();
    }

    @Test
    public void createTransferToken() {
        Token token = payerAccount.createTransferToken(100.0, payeeAccount)
                .setRedeemerUsername(payee.firstUsername())
                .setDescription("book purchase")
                .execute();

        assertThat(token)
                .hasFrom(payer)
                .hasRedeemerUsername(payee.firstUsername())
                .hasAmount(100.0)
                .hasCurrency(payeeAccount.getCurrency())
                .hasNoSignatures();
    }

    @Test
    public void createTransferToken_noDestination() {
        Token token = payerAccount.createTransferToken(100.0, payeeAccount)
                .setRedeemerUsername(payee.firstUsername())
                .setDescription("book purchase")
                .execute();

        assertThat(token)
                .hasFrom(payer)
                .hasRedeemerUsername(payee.firstUsername())
                .hasAmount(100.0)
                .hasCurrency(payeeAccount.getCurrency())
                .hasNoSignatures();
    }

    @Test
    public void createTransferTokenWithUnlinkedAccount() {
        payer.unlinkAccounts(singletonList(payerAccount.getId()));
        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(() -> payerAccount.createTransferToken(100.0, payeeAccount)
                        .setRedeemerUsername(payee.firstUsername())
                        .execute())
                .matches(e -> e.getStatus().getCode() == FAILED_PRECONDITION);
    }

    @Test
    public void createTransferToken_invalidAccount() {
        assertThatExceptionOfType(TransferTokenException.class)
                .isThrownBy(() -> payerAccount.createTransferToken(100.0, payeeAccount)
                        .setAccountId(payeeAccount.getId()) // Wrong account
                        .setRedeemerUsername(payee.firstUsername())
                        .setDescription("book purchase")
                        .execute())
                .matches(e -> e.getStatus() == TransferTokenStatus.FAILURE_SOURCE_ACCOUNT_NOT_FOUND);
    }

    @Test
    public void createTransferToken_invalidCurrency() {
        assertThatExceptionOfType(TransferTokenException.class)
                .isThrownBy(() -> payerAccount.getMember().createTransferToken(100.0, "XXX")
                        .setAccountId(payerAccount.getId())
                        .setRedeemerUsername(payee.firstUsername())
                        .setDescription("book purchase")
                        .execute())
        .matches(e -> e.getStatus() == TransferTokenStatus.FAILURE_INVALID_CURRENCY);
    }

    @Test
    public void createTransferToken_instant_invalidCurrency() {
        assertThatExceptionOfType(TransferTokenException.class)
                .isThrownBy(() -> payerAccount.getMember().createTransferToken(100.0, "XXX")
                        .setAccountId(payerAccount.getId())
                        .setRedeemerUsername(payee.firstUsername())
                        .setDescription("book purchase")
                        .addDestination(TransferEndpoint.newBuilder()
                                .setAccount(BankAccount.newBuilder()
                                        .setToken(BankAccount.Token.newBuilder()
                                                .setMemberId(payeeAccount.getMember().memberId())
                                                .setAccountId(payeeAccount.getId())))
                                .build())
                        .execute())
                .matches(e -> e.getStatus() == TransferTokenStatus.FAILURE_INVALID_CURRENCY);
    }

    @Test
    public void getTransferToken() {
        Token token = payerAccount.createTransferToken(100.0, payeeAccount)
                .setRedeemerUsername(payee.firstUsername())
                .execute();
        assertThat(payer.getToken(token.getId()))
                .hasFrom(payer)
                .hasAmount(100.0)
                .hasCurrency(payeeAccount.getCurrency())
                .hasNoSignatures();
    }

    @Test
    public void getTransferTokens() {
        Token token1 = payerAccount.createTransferToken(123.45, payeeAccount)
                .setRedeemerUsername(payee.firstUsername())
                .execute();
        Token token2 = payerAccount.createTransferToken(678.90, payeeAccount)
                .setRedeemerUsername(payee.firstUsername())
                .execute();
        Token token3 = payerAccount.createTransferToken(100.99, payeeAccount)
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
            payerAccount.createTransferToken(100 + i, payeeAccount)
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
        Token token = payerAccount.createTransferToken(100.0, payeeAccount)
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
                .hasCurrency(payeeAccount.getCurrency());
    }

    @Test
    public void endorseTransferTokenWithUnlinkedAccount() {
        Token token = payerAccount.createTransferToken(100.0, payeeAccount)
                .setRedeemerUsername(payee.firstUsername())
                .execute();
        payer.unlinkAccounts(singletonList(payerAccount.getId()));
        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(() -> payer.endorseToken(token, Key.Level.STANDARD))
                .matches(e -> e.getStatus().getCode() == FAILED_PRECONDITION);
    }

    @Test
    public void cancelTransferToken() {
        Token token = payerAccount.createTransferToken(100.0, payeeAccount)
                .setRedeemerUsername(payee.firstUsername())
                .execute();
        TokenOperationResult result = payer.cancelToken(token);
        assertThat(result.getStatus()).isEqualTo(TokenOperationResult.Status.SUCCESS);

        assertThat(result.getToken())
                .hasNSignatures(2)
                .isCancelledBy(payer, Key.Level.LOW)
                .hasFrom(payer)
                .hasAmount(100.0)
                .hasCurrency(payeeAccount.getCurrency());
    }

    @Test
    public void endorseTransferTokenMoreSignaturesNeeded() {
        Token token = payerAccount.createTransferToken(100.0, payeeAccount)
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
                .hasCurrency(payeeAccount.getCurrency());
    }
}
