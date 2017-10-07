package io.token.sample;

import static io.token.TokenIO.TokenCluster.DEVELOPMENT;
import static io.token.sample.CreateAndEndorseTransferTokenSample.createTransferToken;
import static io.token.sample.GetTransactionsSample.accountGetTransactionSample;
import static io.token.sample.GetTransactionsSample.accountGetTransactionsSample;
import static io.token.sample.GetTransactionsSample.getTransactionSample;
import static io.token.sample.GetTransactionsSample.getTransactionsSample;
import static io.token.sample.RedeemTransferTokenSample.redeemTransferToken;
import static io.token.sample.TestUtil.newAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Account;
import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transfer.TransferProtos.Transfer;

import java.util.List;

import org.junit.Test;

public class GetTransactionsSampleTest {
    @Test
    public void getTransactionsTest() {
        try (TokenIO tokenIO = TokenIO.create(
                DEVELOPMENT,
                "4qY7lqQw8NOl9gng0ZHgT4xdiDqxqoGVutuZwrUYQsI")) {
            Member payer = tokenIO.createMember(newAlias());
            Member payee = tokenIO.createMember(newAlias());

            LinkMemberAndBankSample.linkBankAccounts(payer);
            List<Account> payeeAccounts = LinkMemberAndBankSample.linkBankAccounts(payee);

            Token token = createTransferToken(payer, payee.firstAlias());

            Transfer transfer = redeemTransferToken(
                    payee,
                    payeeAccounts.get(0).id(),
                    token.getId());

            getTransactionsSample(payer);

            Transaction transaction = getTransactionSample(payer, transfer);
            assertThat(transaction.getTokenId()).isEqualTo(token.getId());
        }
    }

    @Test
    public void accountGetTransactionsTest() {
        try (TokenIO tokenIO = TokenIO.create(
                DEVELOPMENT,
                "4qY7lqQw8NOl9gng0ZHgT4xdiDqxqoGVutuZwrUYQsI")) {
            Member payer = tokenIO.createMember(newAlias());
            Member payee = tokenIO.createMember(newAlias());

            LinkMemberAndBankSample.linkBankAccounts(payer);
            List<Account> payeeAccounts = LinkMemberAndBankSample.linkBankAccounts(payee);

            Token token = createTransferToken(payer, payee.firstAlias());

            Transfer transfer = redeemTransferToken(
                    payee,
                    payeeAccounts.get(0).id(),
                    token.getId());

            accountGetTransactionsSample(payer);

            Transaction transaction = accountGetTransactionSample(payer, transfer);
            assertThat(transaction.getTokenId()).isEqualTo(token.getId());
        }
    }
}
