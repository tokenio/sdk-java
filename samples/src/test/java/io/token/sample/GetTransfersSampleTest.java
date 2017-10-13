package io.token.sample;

import static io.token.sample.CreateAndEndorseTransferTokenSample.createTransferToken;
import static io.token.sample.GetTransfersSample.getTransferSample;
import static io.token.sample.GetTransfersSample.getTransferTokensSample;
import static io.token.sample.GetTransfersSample.getTransfersSample;
import static io.token.sample.RedeemTransferTokenSample.redeemTransferToken;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createMemberAndLinkAccounts;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Account;
import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transfer.TransferProtos.Transfer;

import java.util.List;

import org.junit.Test;

public class GetTransfersSampleTest {
    @Test
    public void getTransfersTest() {
        try (TokenIO tokenIO = createClient()) {
            Member payer = createMemberAndLinkAccounts(tokenIO);
            Alias payeeAlias = randomAlias();
            Member payee = tokenIO.createMember(payeeAlias);

            List<Account> payeeAccounts = LinkMemberAndBankSample.linkBankAccounts(payee);

            Token token = createTransferToken(payer, payeeAlias);

            Transfer transfer = redeemTransferToken(
                    payee,
                    payeeAccounts.get(0).id(),
                    token.getId());

            getTransfersSample(payer);
        }
    }

    @Test
    public void getTransferTokensTest() {
        try (TokenIO tokenIO = createClient()) {
            Member payer = createMemberAndLinkAccounts(tokenIO);
            Alias payeeAlias = randomAlias();
            Member payee = tokenIO.createMember(payeeAlias);

            List<Account> payeeAccounts = LinkMemberAndBankSample.linkBankAccounts(payee);

            Token token = createTransferToken(payer, payeeAlias);

            Transfer transfer = redeemTransferToken(
                    payee,
                    payeeAccounts.get(0).id(),
                    token.getId());

            getTransferTokensSample(payer);
        }
    }

    @Test
    public void getTransferTest() {
        try (TokenIO tokenIO = createClient()) {
            Member payer = createMemberAndLinkAccounts(tokenIO);
            Alias payeeAlias = randomAlias();
            Member payee = tokenIO.createMember(payeeAlias);

            List<Account> payeeAccounts = LinkMemberAndBankSample.linkBankAccounts(payee);

            Token token = createTransferToken(payer, payeeAlias);

            Transfer redeemedTransfer = redeemTransferToken(
                    payee,
                    payeeAccounts.get(0).id(),
                    token.getId());

            Transfer gotTransfer = getTransferSample(
                    payee,
                    redeemedTransfer.getId());
            assertThat(gotTransfer).isEqualTo(redeemedTransfer);
        }
    }
}
