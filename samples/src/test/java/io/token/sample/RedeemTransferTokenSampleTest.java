package io.token.sample;

import static io.token.proto.common.testing.Sample.alias;
import static io.token.TokenIO.TokenCluster.DEVELOPMENT;
import static io.token.sample.CreateAndEndorseTransferTokenSample.createTransferToken;
import static io.token.sample.RedeemTransferTokenSample.redeemTransferToken;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Account;
import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transfer.TransferProtos.Transfer;

import java.util.List;
import org.junit.Test;

public class RedeemTransferTokenSampleTest {
    @Test
    public void redeemPaymentTokenTest() {
        try (TokenIO tokenIO = TokenIO.create(DEVELOPMENT)) {
            Member payer = tokenIO.createMember(alias());
            Member payee = tokenIO.createMember(alias());

            LinkMemberAndBankSample.linkBankAccounts(payer);
            List<Account> payeeAccounts = LinkMemberAndBankSample.linkBankAccounts(payee);

            Token token = createTransferToken(payer, payee.firstAlias());

            Transfer transfer = redeemTransferToken(
                    payee,
                    payeeAccounts.get(0).id(),
                    token.getId());
            assertThat(transfer).isNotNull();
        }
    }
}
