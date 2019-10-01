package io.token.sample;

import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.sample.CreateStandingOrderTokenSample.createStandingOrderToken;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createMemberAndLinkAccounts;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.user.Member;
import io.token.user.TokenClient;

import org.junit.Test;

public class CreateStandingOrderTokenSampleTest {
    @Test
    public void createStandingOrderTokenTest() {
        try (TokenClient tokenClient = createClient()) {
            Member payer = createMemberAndLinkAccounts(tokenClient);
            Alias payeeAlias = randomAlias();
            Member payee = tokenClient.createMemberBlocking(payeeAlias);
            Token token = createStandingOrderToken(payer, payeeAlias, STANDARD);
            assertThat(token).isNotNull();
        }
    }
}
