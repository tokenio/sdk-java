package io.token.sample;

import static io.token.TokenIO.TokenCluster.SANDBOX;
import static io.token.proto.common.testing.Sample.alias;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.alias.AliasProtos.Alias;

public final class CreateMemberSample {
    /**
     * Creates and returns a new token member.
     *
     * @return a new Member instance
     */
    public static Member createMember() {
        // Create the client, which communicates with
        // the Token cloud.
        TokenIO tokenIO = TokenIO.builder()
                .connectTo(SANDBOX)
                .build();

        // The alias() method generates a random-nonsense-string alias.
        // "name@token.io" would be more typical than a random string.
        // But if we run this code with the same alias twice,
        // the 2nd time it will fail because the name is taken.
        Alias alias = alias();

        return tokenIO.createMember(alias);
    }
}
