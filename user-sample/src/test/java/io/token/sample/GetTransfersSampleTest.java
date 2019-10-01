package io.token.sample;

import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.sample.CreateTransferTokenSample.createTransferToken;
import static io.token.sample.GetTransfersSample.getTransferSample;
import static io.token.sample.GetTransfersSample.getTransferTokensSample;
import static io.token.sample.GetTransfersSample.getTransfersSample;
import static io.token.sample.RedeemTransferTokenSample.redeemTransferToken;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createMemberAndLinkAccounts;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.user.Account;
import io.token.user.Member;
import io.token.user.TokenClient;

import org.junit.Test;

public class GetTransfersSampleTest {
    @Test
    public void getTransfersTest() {
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

            getTransfersSample(payer);
        }
    }

    @Test
    public void getTransferTokensTest() {
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

            getTransferTokensSample(payer);
        }
    }

    @Test
    public void getTransferTest() {
        try (TokenClient tokenClient = createClient()) {
            Member payer = createMemberAndLinkAccounts(tokenClient);
            Alias payeeAlias = randomAlias();
            Member payee = tokenClient.createMemberBlocking(payeeAlias);

            Account payeeAccount = LinkMemberAndBankSample.linkBankAccounts(payee);

            Token token = createTransferToken(payer, payeeAlias, LOW);

            Transfer redeemedTransfer = redeemTransferToken(
                    payee,
                    payeeAccount.id(),
                    token.getId());

            Transfer gotTransfer = getTransferSample(
                    payee,
                    redeemedTransfer.getId());
            assertThat(gotTransfer).isEqualTo(redeemedTransfer);
        }
    }
}
