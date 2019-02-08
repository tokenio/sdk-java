package io.token.sample;

import static io.token.TokenClient.TokenCluster.DEVELOPMENT;
import static io.token.proto.common.alias.AliasProtos.Alias.Type.EMAIL;
import static io.token.user.util.Util.generateNonce;

import com.google.common.util.concurrent.Uninterruptibles;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.user.Member;
import io.token.user.TokenClient;

import java.util.concurrent.TimeUnit;

/**
 * A set of helper methods used for testing.
 */
public abstract class TestUtil {
    private static String DEV_KEY = "f3982819-5d8d-4123-9601-886df2780f42";

    private TestUtil() {
    }

    /**
     * Create a TokenIO SDK client handy for testing samples.
     *
     * @return client
     */
    public static TokenClient createClient() {
        return TokenClient.create(DEVELOPMENT, DEV_KEY);
    }

    /**
     * Generates random user name to be used for testing.
     *
     * @return random user name
     */
    public static Alias randomAlias() {
        return Alias.newBuilder()
                .setType(EMAIL)
                .setValue("alias-" + generateNonce().toLowerCase() + "+noverify@example.com")
                .build();
    }

    /**
     * Create a Member with a random alias and link bank accounts
     *
     * @return member
     */
    public static Member createMemberAndLinkAccounts(TokenClient client) {
        Alias alias = randomAlias();
        Member member = client.createMemberBlocking(alias);
        LinkMemberAndBankSample.linkBankAccounts(member);
        return member;
    }

    /**
     * Poll until function doesn't assert (once in 0.5 sec with timeout of 1 minute).
     *
     * @param function function
     */
    public static void waitUntil(Runnable function) {
        waitUntil(60000, 500, 1, function);
    }

    /**
     * Poll until function doesn't assert
     * @param timeoutMs give up
     * @param waitTimeMs base wait time
     * @param backOffFactor exponential backoff
     * @param function hope it doesn't assert every time
     */
    public static void waitUntil(
            long timeoutMs,
            long waitTimeMs,
            int backOffFactor,
            Runnable function) {
        for (long start = System.currentTimeMillis(); ; waitTimeMs *= backOffFactor) {
            try {
                function.run();
                return;
            } catch (AssertionError caughtError) {
                if (System.currentTimeMillis() - start < timeoutMs) {
                    Uninterruptibles.sleepUninterruptibly(waitTimeMs, TimeUnit.MILLISECONDS);
                } else {
                    throw caughtError;
                }
            }
        }
    }
}
