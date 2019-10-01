package io.token.sample;

import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.proto.common.token.TokenProtos.TokenSignature.Action.CANCELLED;
import static io.token.sample.CreateTransferTokenSample.createTransferToken;
import static io.token.sample.GetTokensSample.getToken;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createMemberAndLinkAccounts;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.user.Member;
import io.token.user.TokenClient;

import java.util.stream.Collectors;

import org.junit.Test;

public class GetTokensSampleTest {
    @Test
    public void getTokenTest() {
        try (TokenClient tokenClient = createClient()) {
            Member payer = createMemberAndLinkAccounts(tokenClient);
            Alias granteeAlias = randomAlias();
            Member payee = tokenClient.createMemberBlocking(granteeAlias);

            Token token = createTransferToken(payer, granteeAlias, LOW);

            assertThat(getToken(payer, token.getId()).getId())
                    .isEqualTo(token.getId());

            assertThat(getToken(payer, token.getId()).getPayloadSignaturesList()
                    .stream()
                    .filter(sig -> sig.getAction() == CANCELLED)
                    .collect(Collectors.toList()))
                    .isEmpty();

            // cancel token
            payer.cancelTokenBlocking(token);

            // check for CANCELLED signature
            assertThat(getToken(payer, token.getId()).getPayloadSignaturesList()
                    .stream()
                    .map(sig -> sig.getAction() == CANCELLED)
                    .collect(Collectors.toList()))
                    .isNotEmpty();
        }
    }
}
