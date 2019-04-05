package io.token.sample;

import static io.token.proto.common.token.TokenProtos.TokenSignature.Action.CANCELLED;
import static io.token.sample.CreateAndEndorseTransferTokenSample.createTransferToken;
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

            Token token = createTransferToken(payer, granteeAlias);

            assertThat(payee.getTokenBlocking(token.getId()).getId())
                    .isEqualTo(token.getId());

            assertThat(payee.getTokenBlocking(token.getId()).getPayloadSignaturesList()
                    .stream()
                    .filter(sig -> sig.getAction() == CANCELLED)
                    .collect(Collectors.toList()))
                    .isEmpty();

            // endorse token
            payer.cancelTokenBlocking(token);

            // check for ENDORSED signature
            assertThat(payer.getTokenBlocking(token.getId()).getPayloadSignaturesList()
                    .stream()
                    .map(sig -> sig.getAction() == CANCELLED)
                    .collect(Collectors.toList()))
                    .isNotEmpty();
        }
    }
}
