package io.token.sample;

import static io.token.TokenIO.TokenCluster.DEVELOPMENT;
import static io.token.common.Constants.DEV_KEY;
import static io.token.proto.common.alias.AliasProtos.Alias.Type.EMAIL;

import io.token.TokenIO;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.util.Util;

/**
 * A set of helper methods used for testing.
 */
public abstract class TestUtil {
    private TestUtil() {}

    public static TokenIO createClient() {
        return TokenIO.create(DEVELOPMENT, DEV_KEY);
    }

    /**
     * Generates random user name to be used for testing.
     *
     * @return random user name
     */
    public static Alias newAlias() {
        return Alias.newBuilder()
                .setType(EMAIL)
                .setValue("alias-" + Util.generateNonce().toLowerCase() + "@example.com")
                .build();
    }
}
