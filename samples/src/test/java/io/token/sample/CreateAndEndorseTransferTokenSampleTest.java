package io.token.sample;

import static io.token.proto.common.testing.Sample.alias;
import static io.token.TokenIO.TokenCluster.DEVELOPMENT;
import static io.token.sample.CreateAndEndorseTransferTokenSample.createTransferToken;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.token.TokenProtos.Token;

import org.junit.Test;

public class CreateAndEndorseTransferTokenSampleTest {
    @Test
    public void createPaymentTokenTest() {
        try (TokenIO tokenIO = TokenIO.create(DEVELOPMENT)) {
            Member payer = tokenIO.createMember(alias());
            Member payee = tokenIO.createMember(alias());

            LinkMemberAndBankSample.linkBankAccounts(payer);

            Token token = createTransferToken(payer, payee.firstAlias());
            assertThat(token).isNotNull();
        }
    }
}
