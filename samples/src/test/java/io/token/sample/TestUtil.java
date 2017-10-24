package io.token.sample;

import static io.token.TokenIO.TokenCluster.DEVELOPMENT;
import static io.token.proto.common.alias.AliasProtos.Alias.Type.EMAIL;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.util.Util;

/**
 * A set of helper methods used for testing.
 */
public abstract class TestUtil {
    private static String DEV_KEY = "4qY7lqQw8NOl9gng0ZHgT4xdiDqxqoGVutuZwrUYQsI";

    private TestUtil() {
    }

    /**
     * Create a TokenIO SDK client handy for testing samples.
     *
     * @return client
     */
    public static TokenIO createClient() {
        return TokenIO.create(DEVELOPMENT, DEV_KEY);
    }

    /**
     * Generates random user name to be used for testing.
     *
     * @return random user name
     */
    public static Alias randomAlias() {
        return Alias.newBuilder()
                .setType(EMAIL)
                .setValue("alias-" + Util.generateNonce().toLowerCase() + "+noverify@example.com")
                .build();
    }

    /**
     * Create a Member with a random alias and link bank accounts
     *
     * @return member
     */
    public static Member createMemberAndLinkAccounts(TokenIO client) {
        Member member = client.createMember(randomAlias());
        LinkMemberAndBankSample.linkBankAccounts(member);
        return member;
    }

}
