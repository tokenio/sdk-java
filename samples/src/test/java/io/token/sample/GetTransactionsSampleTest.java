package io.token.sample;

import static io.token.sample.CreateAndEndorseTransferTokenSample.createTransferToken;
import static io.token.sample.GetTransactionsSample.accountGetTransactionSample;
import static io.token.sample.GetTransactionsSample.accountGetTransactionsSample;
import static io.token.sample.GetTransactionsSample.getTransactionSample;
import static io.token.sample.GetTransactionsSample.getTransactionsSample;
import static io.token.sample.RedeemTransferTokenSample.redeemTransferToken;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createMemberAndLinkAccounts;
import static io.token.sample.TestUtil.randomAlias;
import static io.token.sample.TestUtil.waitUntil;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Account;
import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transfer.TransferProtos.Transfer;

import java.util.List;

import org.junit.Test;

public class GetTransactionsSampleTest {
    @Test
    public void getTransactionsTest() {
        try (TokenIO tokenIO = createClient()) {
            Member payer = createMemberAndLinkAccounts(tokenIO);
            Alias payeeAlias = randomAlias();
            Member payee = tokenIO.createMember(payeeAlias);
            // wait until alias is processed by the asynchronous verification job (this is needed
            // only for +noverify aliases)
            waitUntil(() -> assertThat(payee.aliases()).contains(payeeAlias));

            Account payeeAccount = LinkMemberAndBankSample.linkBankAccounts(payee);

            Token token = createTransferToken(payer, payeeAlias);

            Transfer transfer = redeemTransferToken(
                    payee,
                    payeeAccount.id(),
                    token.getId());

            getTransactionsSample(payer);

            Transaction transaction = getTransactionSample(payer, transfer);
            assertThat(transaction.getTokenId()).isEqualTo(token.getId());
        }
    }

    @Test
    public void accountGetTransactionsTest() {
        try (TokenIO tokenIO = createClient()) {
            Member payer = createMemberAndLinkAccounts(tokenIO);
            Alias payeeAlias = randomAlias();
            Member payee = tokenIO.createMember(payeeAlias);
            // wait until alias is processed by the asynchronous verification job (this is needed
            // only for +noverify aliases)
            waitUntil(() -> assertThat(payee.aliases()).contains(payeeAlias));

            Account payeeAccount = LinkMemberAndBankSample.linkBankAccounts(payee);

            Token token = createTransferToken(payer, payeeAlias);

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
