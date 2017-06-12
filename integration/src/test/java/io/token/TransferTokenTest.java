package io.token;

import static io.grpc.Status.Code.FAILED_PRECONDITION;
import static io.grpc.Status.Code.PERMISSION_DENIED;
import static io.token.asserts.TokenAssertion.assertThat;
import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.proto.common.token.TokenProtos.TokenOperationResult.Status.MORE_SIGNATURES_NEEDED;
import static io.token.proto.common.token.TokenProtos.TokenOperationResult.Status.SUCCESS;
import static io.token.proto.common.token.TokenProtos.TransferTokenStatus.FAILURE_CUSTOMER_NOT_FOUND;
import static io.token.proto.common.token.TokenProtos.TransferTokenStatus.FAILURE_INVALID_CURRENCY;
import static io.token.testing.sample.Sample.string;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.google.common.collect.ImmutableSet;
import io.grpc.StatusRuntimeException;
import io.token.common.LinkedAccount;
import io.token.common.TokenRule;
import io.token.proto.PagedList;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
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
        Token token = payerAccount
                .createInstantToken(100.0, payeeAccount)
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
        Token token = payerAccount.createInstantToken(100.0, payeeAccount)
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
    public void createTransferToken_idempotentRefId() {
        String refId = string();

        Token token1 = payerAccount.createInstantToken(100.0, payeeAccount)
                .setRefId(refId)
                .setRedeemerUsername(payee.firstUsername())
                .setDescription("book purchase 1")
                .execute();

        Token token2 = payerAccount.createInstantToken(200.0, payeeAccount)
                .setRefId(refId)
                .setRedeemerUsername(payee.firstUsername())
                .setDescription("book purchase 2")
                .execute();

        assertThat(token1)
                .hasFrom(payer)
                .hasRedeemerUsername(payee.firstUsername())
                .hasAmount(100.0)
                .hasCurrency(payeeAccount.getCurrency())
                .hasNoSignatures();

        assertThat(token2).isEqualTo(token1);
    }

    @Test
    public void createTransferToken_sameRefIdDifferentPayer() {
        String refId = string();

        Token token1 = payerAccount.createInstantToken(100.0, payeeAccount)
                .setRefId(refId)
                .setRedeemerUsername(payee.firstUsername())
                .setDescription("book purchase 1")
                .execute();

        Token token2 = payeeAccount.createInstantToken(200.0, payerAccount)
                .setRefId(refId)
                .setRedeemerUsername(payer.firstUsername())
                .setDescription("book purchase 2")
                .execute();

        assertThat(token1)
                .hasFrom(payer)
                .hasRedeemerUsername(payee.firstUsername())
                .hasAmount(100.0)
                .hasCurrency(payeeAccount.getCurrency())
                .hasNoSignatures();

        assertThat(token2)
                .hasFrom(payee)
                .hasRedeemerUsername(payer.firstUsername())
                .hasAmount(200.0)
                .hasCurrency(payerAccount.getCurrency())
                .hasNoSignatures();
    }

    @Test
    public void createTransferTokenWithUnlinkedAccount() {
        payer.unlinkAccounts(singletonList(payerAccount.getId()));
        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(() -> payerAccount.createInstantToken(100.0, payeeAccount)
                        .setRedeemerUsername(payee.firstUsername())
                        .execute())
                .matches(e -> e.getStatus().getCode() == FAILED_PRECONDITION);
    }

    @Test
    public void createTransferToken_invalidAccount_source() {
        assertThatExceptionOfType(TransferTokenException.class)
                .isThrownBy(() -> payerAccount.createInstantToken(100.0, payeeAccount)
                        .setAccountId(payeeAccount.getId()) // Wrong account
                        .setRedeemerUsername(payee.firstUsername())
                        .setDescription("book purchase")
                        .execute())
                .matches(e -> e.getStatus() == FAILURE_CUSTOMER_NOT_FOUND);
    }

    @Test
    public void createTransferToken_invalidAccount_destination() {
        assertThatExceptionOfType(TransferTokenException.class)
                .isThrownBy(() -> payerAccount.getMember()
                        .createTransferToken(100.0, payeeAccount.getCurrency())
                        .setAccountId(payerAccount.getId())
                        .setRedeemerUsername(payee.firstUsername())
                        .setDescription("book purchase")
                        .addDestination(TransferEndpoint.newBuilder()
                                // Invalid account.
                                .setAccount(
                                        rule.invalidLinkedAccount().testAccount().getBankAccount())
                                .build())
                        .execute())
                .matches(e -> e.getStatus() == FAILURE_CUSTOMER_NOT_FOUND);
    }

    @Test
    public void createTransferToken_invalidCurrency() {
        assertThatExceptionOfType(TransferTokenException.class)
                .isThrownBy(() -> payerAccount.getMember().createTransferToken(100.0, "XXX")
                        .setAccountId(payerAccount.getId())
                        .setRedeemerUsername(payee.firstUsername())
                        .setDescription("book purchase")
                        .execute())
                .matches(e -> e.getStatus() == FAILURE_INVALID_CURRENCY);
    }

    @Test
    public void createTransferToken_instant_invalidCurrency() {
        assertThatExceptionOfType(TransferTokenException.class)
                .isThrownBy(() -> payerAccount.getMember().createTransferToken(100.0, "XXX")
                        .setAccountId(payerAccount.getId())
                        .setRedeemerUsername(payee.firstUsername())
                        .setDescription("book purchase")
                        .addDestination(Destinations.token(
                                payeeAccount.getMember().memberId(),
                                payeeAccount.getId()))
                        .execute())
                .matches(e -> e.getStatus() == FAILURE_INVALID_CURRENCY);
    }

    @Test
    public void getTransferToken() {
        Token token = payerAccount.createInstantToken(100.0, payeeAccount)
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
        Token token1 = payerAccount.createInstantToken(123.45, payeeAccount)
                .setRedeemerUsername(payee.firstUsername())
                .execute();
        Token token2 = payerAccount.createInstantToken(678.90, payeeAccount)
                .setRedeemerUsername(payee.firstUsername())
                .execute();
        Token token3 = payerAccount.createInstantToken(100.99, payeeAccount)
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
            payerAccount.createInstantToken(100 + i, payeeAccount)
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
        Token token = payerAccount.createInstantToken(100.0, payeeAccount)
                .setRedeemerUsername(payee.firstUsername())
                .execute();
        TokenOperationResult result = payer.endorseToken(token, STANDARD);
        assertThat(result.getStatus())
                .isEqualTo(SUCCESS);
        assertThat(result.getToken())
                .hasNSignatures(2)
                .isEndorsedBy(payer, STANDARD)
                .hasFrom(payer)
                .hasAmount(100.0)
                .hasCurrency(payeeAccount.getCurrency());
    }

    @Test
    public void endorseTransferTokenWithUnlinkedAccount() {
        Token token = payerAccount.createInstantToken(100.0, payeeAccount)
                .setRedeemerUsername(payee.firstUsername())
                .execute();
        payer.unlinkAccounts(singletonList(payerAccount.getId()));
        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(() -> payer.endorseToken(token, STANDARD))
                .matches(e -> e.getStatus().getCode() == FAILED_PRECONDITION);
    }

    @Test
    public void cancelTransferToken() {
        Token token = payerAccount.createInstantToken(100.0, payeeAccount)
                .setRedeemerUsername(payee.firstUsername())
                .execute();
        TokenOperationResult result = payer.cancelToken(token);
        assertThat(result.getStatus()).isEqualTo(SUCCESS);

        assertThat(result.getToken())
                .hasNSignatures(2)
                .isCancelledBy(payer, LOW)
                .hasFrom(payer)
                .hasAmount(100.0)
                .hasCurrency(payeeAccount.getCurrency());
    }

    @Test
    public void endorseTransferTokenMoreSignaturesNeeded() {
        Token token = payerAccount.createInstantToken(100.0, payeeAccount)
                .setRedeemerUsername(payee.firstUsername())
                .execute();
        TokenOperationResult result = payer.endorseToken(token, LOW);

        assertThat(result.getStatus())
                .isEqualTo(MORE_SIGNATURES_NEEDED);
        assertThat(result.getToken())
                .hasNSignatures(1)
                .isEndorsedBy(payer, LOW)
                .hasFrom(payer)
                .hasAmount(100.0)
                .hasCurrency(payeeAccount.getCurrency());
    }
}
