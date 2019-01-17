package io.token.sample;

import static io.token.TokenClient.TokenCluster.DEVELOPMENT;
import static io.token.proto.common.alias.AliasProtos.Alias.Type.EMAIL;
import static io.token.proto.common.member.MemberProtos.CreateMemberType.PERSONAL;

import com.google.common.util.concurrent.Uninterruptibles;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.security.SecurityProtos;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.tpp.TokenClient;
import io.token.user.AccessTokenBuilder;
import io.token.util.Util;

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
        io.token.user.Member member = client.createMemberBlocking(alias, PERSONAL);
        member.createTestBankAccountBlocking(1000.0, "EUR");
        return member;
    }

    /**
     * Pulled from User SDK Sample. Creates a transfer token.
     *
     * @param payer payer user member
     * @param payeeAlias payee alias
     * @return token
     */
    public static Token createTransferToken(
            io.token.user.Member payer,
            Alias payeeAlias) {
        // We'll use this as a reference ID. Normally, a payer who
        // explicitly sets a reference ID would use an ID from a db.
        // E.g., a bill-paying service might use ID of a "purchase".
        // We don't have a db, so we fake it with a random string:
        String purchaseId = io.token.user.util.Util.generateNonce();

        // Create a transfer token.
        Token transferToken = payer.createTransferToken(
                100.0, // amount
                "EUR")  // currency
                // source account:
                .setAccountId(payer.getAccountsBlocking().get(0).id())
                // payee token alias:
                .setToAlias(payeeAlias)
                // optional description:
                .setDescription("Book purchase")
                // ref id (if not set, will get random ID)
                .setRefId(purchaseId)
                .execute();

        // Payer endorses a token to a payee by signing it
        // with her secure private key.
        transferToken = payer.endorseTokenBlocking(
                transferToken,
                Key.Level.STANDARD).getToken();

        return transferToken;
    }

    /**
     * Pulled from User SDK sample. Creates an access token.
     *
     * @param grantor grantor user member
     * @param accountId ID of account to grant access to
     * @param granteeAlias grantee alias
     * @return token
     */
    public static Token createAccessToken(
            io.token.user.Member grantor,
            String accountId,
            Alias granteeAlias) {
        // Create an access token for the grantee to access bank
        // account names of the grantor.
        Token accessToken = grantor.createAccessTokenBlocking(
                AccessTokenBuilder
                        .create(granteeAlias)
                        .forAccount(accountId)
                        .forAccountBalances(accountId));

        // Grantor endorses a token to a grantee by signing it
        // with her secure private key.
        accessToken = grantor.endorseTokenBlocking(
                accessToken,
                Key.Level.STANDARD).getToken();

        return accessToken;
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
