package io.token.sample;

import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.sample.CreateTransferTokenSample.createTransferToken;
import static io.token.sample.GetTransactionsSample.accountGetTransactionSample;
import static io.token.sample.GetTransactionsSample.accountGetTransactionsSample;
import static io.token.sample.GetTransactionsSample.getTransactionSample;
import static io.token.sample.GetTransactionsSample.getTransactionsByDateSample;
import static io.token.sample.GetTransactionsSample.getTransactionsSample;
import static io.token.sample.RedeemTransferTokenSample.redeemTransferToken;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createMemberAndLinkAccounts;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.user.Account;
import io.token.user.Member;
import io.token.user.TokenClient;

import org.junit.Test;

public class GetTransactionsSampleTest {
    @Test
    public void getTransactionsTest() {
        try (TokenClient tokenClient = createClient()) {
            Member payer = createMemberAndLinkAccounts(tokenClient);
            Alias payeeAlias = randomAlias();
            Member payee = tokenClient.createMemberBlocking(payeeAlias);

            Account payeeAccount = LinkMemberAndBankSample.linkBankAccounts(payee);

            Token token = createTransferToken(payer, payeeAlias, LOW);

            Transfer transfer = redeemTransferToken(
                    payee,
                    payeeAccount.id(),
                    token.getId());

            getTransactionsSample(payer);
            getTransactionsByDateSample(payer);

            Transaction transaction = getTransactionSample(payer, transfer);
            assertThat(transaction.getTokenId()).isEqualTo(token.getId());
        }
    }

    @Test
    public void accountGetTransactionsTest() {
        try (TokenClient tokenClient = createClient()) {
            Member payer = createMemberAndLinkAccounts(tokenClient);
            Alias payeeAlias = randomAlias();
            Member payee = tokenClient.createMemberBlocking(payeeAlias);

            Account payeeAccount = LinkMemberAndBankSample.linkBankAccounts(payee);

            Token token = createTransferToken(payer, payeeAlias, LOW);

            Transfer transfer = redeemTransferToken(
                    payee,
                    payeeAccount.id(),
                    token.getId());

            accountGetTransactionsSample(payer);

            Transaction transaction = accountGetTransactionSample(payer, transfer);
            assertThat(transaction.getTokenId()).isEqualTo(token.getId());
        }
    }
}
