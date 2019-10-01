package io.token.sample;

import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.proto.common.token.TokenProtos.TokenSignature.Action.CANCELLED;
import static io.token.sample.CreateTransferTokenSample.createTransferToken;
import static io.token.sample.GetTokensSample.getToken;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createUserMember;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;

import java.util.stream.Collectors;

import org.junit.Test;

public class GetTokensSampleTest {
    @Test
    public void getTokenTest() {
        try (TokenClient tokenClient = createClient()) {
            io.token.user.Member payer = createUserMember();
            Alias payeeAlias = randomAlias();
            Member payee = tokenClient.createMemberBlocking(payeeAlias);

            Token token = createTransferToken(payer, payeeAlias, LOW);

            assertThat(getToken(payee, token.getId()).getId())
                    .isEqualTo(token.getId());

            assertThat(getToken(payee, token.getId()).getPayloadSignaturesList()
                    .stream()
                    .filter(sig -> sig.getAction() == CANCELLED)
                    .collect(Collectors.toList()))
                    .isEmpty();

            // cancel token
            payee.cancelTokenBlocking(token);

            // check for CANCELLED signature
            assertThat(getToken(payee, token.getId()).getPayloadSignaturesList()
                    .stream()
                    .map(sig -> sig.getAction() == CANCELLED)
                    .collect(Collectors.toList()))
                    .isNotEmpty();
        }
    }
}
