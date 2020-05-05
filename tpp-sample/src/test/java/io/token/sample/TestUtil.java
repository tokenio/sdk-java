package io.token.sample;

import static io.token.TokenClient.TokenCluster.DEVELOPMENT;
import static io.token.proto.common.alias.AliasProtos.Alias.Type.EMAIL;

import com.google.common.util.concurrent.Uninterruptibles;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.security.CryptoEngineFactory;
import io.token.security.KeyStore;
import io.token.tpp.TokenClient;
import io.token.util.Util;

import java.util.concurrent.TimeUnit;

/**
 * A set of helper methods used for testing.
 */
public abstract class TestUtil {
    private static String DEV_KEY = "f3982819-5d8d-4123-9601-886df2780f42";
    private static String TOKEN_REALM = "token";

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

    public static TokenClient createClient(CryptoEngineFactory cryptoEngineFactory) {
        return TokenClient.create(DEVELOPMENT, DEV_KEY, cryptoEngineFactory);
    }

    /**
     * Generates random user name to be used for testing.
     *
     * @return random user name
     */
    public static Alias randomAlias() {
        return Alias.newBuilder()
                .setType(EMAIL)
                .setRealm(TOKEN_REALM)
                .setValue("alias-" + Util.generateNonce().toLowerCase() + "+noverify@example.com")
                .build();
    }

    /**
     * Create a Member with a random alias and link bank accounts
     *
     * @return member
     */
    public static io.token.user.Member createUserMember() {
        io.token.user.TokenClient client = io.token.user.TokenClient.create(DEVELOPMENT, DEV_KEY);
        Alias alias = randomAlias();
        io.token.user.Member member = client.createMemberBlocking(alias);
        member.createTestBankAccountBlocking(1000.0, "EUR");
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
