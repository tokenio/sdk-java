package io.token.sample;

import static io.token.sample.CreateAndEndorseAccessTokenSample.createAccessToken;
import static io.token.sample.ReplaceAccessTokenSample.findAccessToken;
import static io.token.sample.ReplaceAccessTokenSample.replaceAccessToken;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.randomAlias;
import static io.token.sample.TestUtil.waitUntil;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos.Token;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.Test;

public class ReplaceAccessTokenSampleTest {
    private static final int TOKEN_LOOKUP_TIMEOUT_MS = 60000;
    private static final int TOKEN_LOOKUP_POLL_FREQUENCY_MS = 5000;

    @Test
    public void getAccessTokensTest() {
        System.out.printf("\nTEST getAccessTokensTest \n");
        try (TokenIO tokenIO = createClient()) {
            Member grantor = tokenIO.createMember(randomAlias());
            Alias granteeAlias = randomAlias();
            Member grantee = tokenIO.createMember(granteeAlias);
            System.out.printf("  createAccessToken() \n");
            Token createdToken = createAccessToken(grantor, granteeAlias);
            waitUntil(TOKEN_LOOKUP_TIMEOUT_MS, TOKEN_LOOKUP_POLL_FREQUENCY_MS, 1, () -> {
                Optional<Token> foundToken = findAccessToken(grantor, granteeAlias);
                assertThat(foundToken).isPresent();
                assertThat(foundToken.get().getId()).isEqualTo(createdToken.getId());
            });
        }
    }

    @Test
    public void replaceAccessTokenTest() {
        System.out.printf("\nTEST replaceAccessTokenTest \n");
        try (TokenIO tokenIO = createClient()) {
            Member grantor = tokenIO.createMember(randomAlias());
            Alias granteeAlias = randomAlias();
            Member grantee = tokenIO.createMember(granteeAlias);
            System.out.printf("  createAccessToken() \n");
            Token createdToken = createAccessToken(grantor, granteeAlias);
            System.out.printf("  first waitUntil loop \n");
            ArrayList<Token> foundList = new ArrayList();
            waitUntil(TOKEN_LOOKUP_TIMEOUT_MS, TOKEN_LOOKUP_POLL_FREQUENCY_MS, 2, () -> {
                System.out.printf("  + waitUntil iter \n");
                Optional<Token> foundToken = findAccessToken(grantor, granteeAlias);
                assertThat(foundToken).isPresent();
                foundList.add(foundToken.get());
            });
            System.out.printf("  inner waitUntil loop \n");
            waitUntil(TOKEN_LOOKUP_TIMEOUT_MS, TOKEN_LOOKUP_POLL_FREQUENCY_MS, 2, () -> {
                System.out.printf("  + waitUntil iter \n");
                assertThatCode(() -> {
                    replaceAccessToken(grantor, granteeAlias, foundList.get(0));
                }).doesNotThrowAnyException();
            });
            System.out.printf("  3rd waitUntil loop \n");
            waitUntil(TOKEN_LOOKUP_TIMEOUT_MS, TOKEN_LOOKUP_POLL_FREQUENCY_MS, 2, () -> {
                System.out.printf("  + waitUntil iter \n");
                Optional<Token> foundToken = findAccessToken(grantor, granteeAlias);
                assertThat(foundToken).isPresent();
                assertThat(foundToken.get().getPayload().getAccess().getResourcesCount())
                        .isEqualTo(2);
            });
        }
    }

    @Test
    public void replaceAndEndorseAccessTokenTest() {
        System.out.printf("\nTEST replaceAndEndorseAccessTokenTest \n");
        try (TokenIO tokenIO = createClient()) {
            Member grantor = tokenIO.createMember(randomAlias());
            Alias granteeAlias = randomAlias();
            Member grantee = tokenIO.createMember(granteeAlias);
            System.out.printf("  createAccessToken() \n");
            Token createdToken = createAccessToken(grantor, granteeAlias);
            System.out.printf("  first waitUntil loop \n");
            ArrayList<Token> foundList = new ArrayList();
            waitUntil(TOKEN_LOOKUP_TIMEOUT_MS, TOKEN_LOOKUP_POLL_FREQUENCY_MS, 2, () -> {
                System.out.printf("  + waitUntil iter \n");
                Optional<Token> foundToken = findAccessToken(grantor, granteeAlias);
                assertThat(foundToken).isPresent();
                foundList.add(foundToken.get());
            });
            System.out.printf("  2nd waitUntil loop \n");
            waitUntil(TOKEN_LOOKUP_TIMEOUT_MS, TOKEN_LOOKUP_POLL_FREQUENCY_MS, 2, () -> {
                System.out.printf("  + waitUntil iter \n");
                System.out.printf("  inner waitUntil loop \n");
                assertThatCode(() -> {
                    ReplaceAccessTokenSample.replaceAndEndorseAccessToken(
                            grantor,
                            granteeAlias,
                            foundList.get(0));
                }).doesNotThrowAnyException();
            });
            System.out.printf("  2nd waitUntil loop \n");
            waitUntil(TOKEN_LOOKUP_TIMEOUT_MS, TOKEN_LOOKUP_POLL_FREQUENCY_MS, 2, () -> {
                System.out.printf("  + waitUntil iter \n");
                Optional<Token> foundToken = findAccessToken(grantor, granteeAlias);
                assertThat(foundToken).isPresent();
                assertThat(foundToken.get().getPayload().getAccess().getResourcesCount())
                        .isEqualTo(2);
            });
        }
    }
}
