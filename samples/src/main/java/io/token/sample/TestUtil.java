package io.token.sample;

import static io.token.proto.common.alias.AliasProtos.Alias.Type.EMAIL;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.util.Util;

/**
 * A set of helper methods used for testing.
 */
public abstract class TestUtil {
    private TestUtil() {}

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
