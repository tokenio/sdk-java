package io.token.sample;

import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.sample.CreateTransferTokenSample.createTransferToken;
import static io.token.sample.GetTransfersSample.getTransferSample;
import static io.token.sample.GetTransfersSample.getTransferTokensSample;
import static io.token.sample.GetTransfersSample.getTransfersSample;
import static io.token.sample.RedeemTransferTokenSample.redeemTransferToken;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createUserMember;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Account;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;

import org.junit.Test;

public class GetTransfersSampleTest {
    @Test
    public void getTransfersTest() {
        try (TokenClient tokenClient = createClient()) {
            io.token.user.Member payer = createUserMember();
            Alias payeeAlias = randomAlias();
            Member payee = tokenClient.createMemberBlocking(payeeAlias);

            Account payeeAccount = payee.createTestBankAccountBlocking(1000, "EUR");

            Token token = createTransferToken(payer, payeeAlias, LOW);

            Transfer transfer = redeemTransferToken(
                    payee,
                    payeeAccount.id(),
                    token.getId());

            getTransfersSample(payee);
        }
    }

    @Test
    public void getTransferTokensTest() {
        try (TokenClient tokenClient = createClient()) {
            io.token.user.Member payer = createUserMember();
            Alias payeeAlias = randomAlias();
            Member payee = tokenClient.createMemberBlocking(payeeAlias);

            Account payeeAccount = payee.createTestBankAccountBlocking(1000, "EUR");

            Token token = createTransferToken(payer, payeeAlias, LOW);

            Transfer transfer = redeemTransferToken(
                    payee,
                    payeeAccount.id(),
                    token.getId());

            getTransferTokensSample(payee);
        }
    }

    @Test
    public void getTransferTest() {
        try (TokenClient tokenClient = createClient()) {
            io.token.user.Member payer = createUserMember();
            Alias payeeAlias = randomAlias();
            Member payee = tokenClient.createMemberBlocking(payeeAlias);

            Account payeeAccount = payee.createTestBankAccountBlocking(1000, "EUR");

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
