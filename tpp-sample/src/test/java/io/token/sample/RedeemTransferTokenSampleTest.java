package io.token.sample;

import static io.token.sample.CreateAndEndorseTransferTokenSample.createTransferToken;
import static io.token.sample.RedeemTransferTokenSample.redeemTransferToken;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createMemberAndLinkAccounts;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Account;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;

import org.junit.Test;

public class RedeemTransferTokenSampleTest {
    @Test
    public void redeemPaymentTokenTest() {
        try (TokenClient tokenClient = createClient()) {
            Member payer = createMemberAndLinkAccounts(tokenClient);
            Alias payeeAlias = randomAlias();
            Member payee = tokenClient.createMemberBlocking(payeeAlias);

            Account payeeAccount = payee.createAndLinkTestBankAccountBlocking(1000, "EUR");

            Token token = createTransferToken(payer, payeeAlias);

            Transfer transfer = redeemTransferToken(
                    payee,
                    payeeAccount.id(),
                    token.getId());
            assertThat(transfer).isNotNull();
        }
    }
}
