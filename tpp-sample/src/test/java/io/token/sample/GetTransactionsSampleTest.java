package io.token.sample;

import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.sample.RedeemTransferTokenSample.redeemTransferToken;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createTransferToken;
import static io.token.sample.TestUtil.createUserMember;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Account;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;

import org.junit.Test;

public class GetTransactionsSampleTest {
    @Test
    public void getTransactionsTest() {
        try (TokenClient tokenClient = createClient()) {
            io.token.user.Member payer = createUserMember();
            Alias payeeAlias = randomAlias();
            Member payee = tokenClient.createMemberBlocking(payeeAlias);

            Account payeeAccount = payee.createTestBankAccountBlocking(1000, "EUR");

            Token token = createTransferToken(payer, payeeAlias);

            Transfer transfer = redeemTransferToken(
                    payee,
                    payeeAccount.id(),
                    token.getId());

            String transactionId = transfer.getTransactionId();
            Transaction transaction = payer.getTransactionBlocking(
                    payer.getAccountsBlocking().get(0).id(),
                    transactionId,
                    STANDARD);
            assertThat(transaction.getTokenId()).isEqualTo(token.getId());
        }
    }

    @Test
    public void accountGetTransactionsTest() {
        try (TokenClient tokenClient = createClient()) {
            io.token.user.Member payer = createUserMember();
            Alias payeeAlias = randomAlias();
            Member payee = tokenClient.createMemberBlocking(payeeAlias);

            Account payeeAccount = payee.createTestBankAccountBlocking(1000, "EUR");

            Token token = createTransferToken(payer, payeeAlias);

            Transfer transfer = redeemTransferToken(
                    payee,
                    payeeAccount.id(),
                    token.getId());

            Account account = payer.getAccountsBlocking().get(0);
            Transaction transaction = account.getTransactionBlocking(
                    transfer.getTransactionId(),
                    STANDARD);
            assertThat(transaction.getTokenId()).isEqualTo(token.getId());
        }
    }
}
